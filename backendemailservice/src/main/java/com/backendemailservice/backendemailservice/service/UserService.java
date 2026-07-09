package com.backendemailservice.backendemailservice.service;

import com.backendemailservice.backendemailservice.config.DiscordOAuthProperties;
import com.backendemailservice.backendemailservice.dto.AuthResponseDto;
import com.backendemailservice.backendemailservice.dto.DiscordExchangeResponseDto;
import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.exception.InvalidEmailDomainException;
import com.backendemailservice.backendemailservice.exception.UserAlreadyExistsException;
import com.backendemailservice.backendemailservice.exception.InvalidFileFormatException;
import com.backendemailservice.backendemailservice.exception.UserNotFoundException;
import com.backendemailservice.backendemailservice.repository.UserRepository;
import com.backendemailservice.backendemailservice.util.JwtUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

// implements IUserService — depend on abstractions
@Service
public class UserService implements IUserService {

    // constructor injection instead of field injection
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String DISCORD_TICKET_PREFIX = "discord_ticket:";
    private static final long DISCORD_TICKET_TTL_SECONDS = 60;
    private static final String DISCORD_STATE_PREFIX = "discord_state:";
    private static final long DISCORD_STATE_TTL_MINUTES = 5;

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final DiscordOAuthProperties discordProperties;
    private final StringRedisTemplate redisTemplate;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, DiscordOAuthProperties discordProperties,
                       StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.discordProperties = discordProperties;
        this.redisTemplate = redisTemplate;
    }

    // authenticate — validates credentials and returns JWT token
    @Override
    @Transactional(readOnly = true)
    public String authenticate(String email, String password) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UserNotFoundException("User not found");
        }
        return jwtUtil.generateToken(email);
    }

    // register — validates domain/duplicates, creates user, returns JWT + refresh token
    // refresh token stored atomically with user creation
    @Override
    @Transactional
    public AuthResponseDto register(String email, String password) {
        if (repository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException("User already exists");
        }
        if (!email.endsWith("@seamail.com")) {
            throw new InvalidEmailDomainException("Emails must end with @seamail.com");
        }
        User user = new User(email, passwordEncoder.encode(password));
        repository.save(user);
        String accessToken = jwtUtil.generateToken(email);
        String refreshToken = generateAndStoreRefreshToken(email);
        return new AuthResponseDto(accessToken, refreshToken);
    }

    // generate and store a refresh token in Redis with 7-day TTL
    @Override
    public String generateAndStoreRefreshToken(String email) {
        String refreshToken = jwtUtil.generateRefreshToken();
        String key = REFRESH_TOKEN_PREFIX + refreshToken;
        redisTemplate.opsForValue().set(key, email, 7, TimeUnit.DAYS);
        return refreshToken;
    }

    // validate refresh token from Redis, issue new access token, rotate refresh token 
    // Uses delete() as an atomic claim so concurrent refreshes with the same token
    // only succeed once - the loser sees the key already gone and is rejected.
    @Override
    public AuthResponseDto refreshAccessToken(String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + refreshToken;
        String email = redisTemplate.opsForValue().get(key);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid or expired refresh token");
        }
        Boolean claimed = redisTemplate.delete(key);
        if (!Boolean.TRUE.equals(claimed)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Refresh token already used");
        }
        String newAccessToken = jwtUtil.generateToken(email);
        String newRefreshToken = generateAndStoreRefreshToken(email);
        return new AuthResponseDto(newAccessToken, newRefreshToken);
    }

    // add @Transactional on write operations 
    @Override
    @Transactional
    public void createUser(User user) {
        String saltedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(saltedPassword);
        repository.save(user);
    }

    public Optional<User> findAndValidateUser(String email, String password) {
        Optional<User> optionalUser = repository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    // add @Transactional(readOnly = true)
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findUser(String email, String password) {
        return findAndValidateUser(email, password);
    }

    // add @Transactional(readOnly = true)
    @Override
    @Transactional(readOnly = true)
    public Optional<User> foundReceiver(String email) {
        return repository.foundReceiver(email);
    }

    @Override
    @Transactional
    public void deleteUserAccount(String authenticatedEmail, String requestedEmail) {
        if (!authenticatedEmail.equals(requestedEmail)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        revokeRefreshTokens(requestedEmail);
        repository.deleteById(requestedEmail);
    }

    @Override
    @Transactional
    public void changeUserPassword(String authenticatedEmail, String requestedEmail,
                                   String currentPassword, String newPassword) {
        if (!authenticatedEmail.equals(requestedEmail)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        User user = repository.findByEmail(requestedEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found."));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        repository.save(user);
        revokeRefreshTokens(requestedEmail);
    }

    @Override
    @Transactional
    public void updateLanguage(String authenticatedEmail, String requestedEmail, String language) {
        if (!authenticatedEmail.equals(requestedEmail)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        User user = repository.findByEmail(requestedEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found."));
        user.setLanguage(language);
        repository.save(user);
    }

    @Override
    @Transactional
    public boolean uploadProfilePicture(String email, byte[] profilePicture) {
        if (profilePicture == null || profilePicture.length < 8) {
            throw new InvalidFileFormatException("File is empty or invalid.");
        }

        boolean isPng = (profilePicture[0] == (byte) 0x89 &&
                         profilePicture[1] == (byte) 0x50 &&
                         profilePicture[2] == (byte) 0x4E &&
                         profilePicture[3] == (byte) 0x47);

        boolean isJpeg = (profilePicture[0] == (byte) 0xFF &&
                          profilePicture[1] == (byte) 0xD8 &&
                          profilePicture[2] == (byte) 0xFF);

        if (!isPng && !isJpeg) {
            throw new InvalidFileFormatException("Only PNG and JPEG images are allowed.");
        }

        if (profilePicture.length > 5 * 1024 * 1024) {
            throw new InvalidFileFormatException("Image exceeds the 5MB size limit.");
        }

        User user = repository.findById(email)
              .orElseThrow(() -> new UserNotFoundException("User not found."));

        user.setProfilePicture(profilePicture);
        repository.save(user);

        return true;
    }

    // auth-check overload
    @Override
    @Transactional
    public void uploadProfilePicture(String authenticatedEmail, String targetEmail, byte[] picture) {
        if (!authenticatedEmail.equals(targetEmail)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        uploadProfilePicture(targetEmail, picture);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] fetchProfilePicture(String authenticatedEmail, String targetEmail) {
        if (!authenticatedEmail.equals(targetEmail)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        User user = repository.findById(targetEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found."));
        return user.getProfilePicture();
    }

    // Generate and store a CSRF state nonce in Redis (5min TTL).
    // The frontend fetches this before redirecting to Discord.
    @Override
    public String generateDiscordState() {
        String state = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(DISCORD_STATE_PREFIX + state, state,
                DISCORD_STATE_TTL_MINUTES, TimeUnit.MINUTES);
        return state;
    }

    // Validate and consume a CSRF state nonce. Throws 401 if not found in Redis.
    @Override
    public void validateDiscordState(String state) {
        String key = DISCORD_STATE_PREFIX + state;
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null || !stored.equals(state)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid or expired OAuth state");
        }
        redisTemplate.delete(key);
    }

    // Revoke all outstanding refresh tokens for a user by scanning Redis keys.
    // Called on account deletion and password change so that already-issued
    // tokens cannot mint new access tokens after the credential change.
    private void revokeRefreshTokens(String email) {
        Set<String> keys = redisTemplate.keys(REFRESH_TOKEN_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            String storedEmail = redisTemplate.opsForValue().get(key);
            if (email.equals(storedEmail)) {
                redisTemplate.delete(key);
            }
        }
    }

    // Discord OAuth flow — token exchange, user info fetch, JWT generation,
    // conditional user creation, ticket-based redirect URL construction.
    // Not @Transactional: the Discord HTTP calls (RestTemplate) must not hold a
    // DB connection/transaction open. Only the DB write is wrapped below.
    @Override
    public String processDiscordOAuth(String code, String state, String allowedOrigin) {
        validateDiscordState(state);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", discordProperties.getClientId());
        requestBody.add("client_secret", discordProperties.getClientSecret());
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("code", code);
        requestBody.add("redirect_uri", discordProperties.getRedirectUri());

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> tokenResponse = restTemplate.exchange(
                discordProperties.getTokenUrl(), HttpMethod.POST, requestEntity, Map.class
        );

        if (!tokenResponse.getStatusCode().is2xxSuccessful()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Failed to get access token from Discord.");
        }

        String accessToken = (String) tokenResponse.getBody().get("access_token");

        headers.clear();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> userRequestEntity = new HttpEntity<>(headers);

        ResponseEntity<Map> userResponse = restTemplate.exchange(
                discordProperties.getApiUrl() + "/users/@me", HttpMethod.GET, userRequestEntity, Map.class
        );

        if (!userResponse.getStatusCode().is2xxSuccessful()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Failed to fetch user info from Discord.");
        }

        Map<String, Object> userInfo = userResponse.getBody();
        String email = (String) userInfo.get("email");

        if (email == null || !email.endsWith("@seamail.com")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Discord account email must end with @seamail.com");
        }

        // Only the DB user-creation write is here; repository methods are
        // individually transactional via Spring Data JPA.
        if (repository.findByEmail(email).isEmpty()) {
            String uniqueId = UUID.randomUUID().toString();
            User user = new User(email, "SignedinWithDiscord" + uniqueId);
            repository.save(user);
        }

        // Store an opaque, single-use ticket in Redis bound to the email and
        // redirect with ?code=<ticket>. The SPA exchanges the ticket via
        // POST /api/v1/auth/exchange to obtain the tokens. This keeps JWTs and
        // the email out of the URL (browser history, server logs, Referer).
        String ticket = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(DISCORD_TICKET_PREFIX + ticket, email,
                DISCORD_TICKET_TTL_SECONDS, TimeUnit.SECONDS);

        return allowedOrigin + "/home?code=" + URLEncoder.encode(ticket, StandardCharsets.UTF_8);
    }

    // Exchange a Discord OAuth ticket for access + refresh tokens.
    // Uses delete() as an atomic claim so concurrent exchanges with the same
    // ticket only succeed once - the loser sees the key already gone and is rejected.
    @Override
    public DiscordExchangeResponseDto exchangeDiscordTicket(String ticket) {
        String key = DISCORD_TICKET_PREFIX + ticket;
        String email = redisTemplate.opsForValue().get(key);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Invalid or expired Discord ticket");
        }
        Boolean claimed = redisTemplate.delete(key);
        if (!Boolean.TRUE.equals(claimed)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Discord ticket already used");
        }
        String newAccessToken = jwtUtil.generateToken(email);
        String newRefreshToken = generateAndStoreRefreshToken(email);
        return new DiscordExchangeResponseDto(newAccessToken, newRefreshToken, email);
    }

}
