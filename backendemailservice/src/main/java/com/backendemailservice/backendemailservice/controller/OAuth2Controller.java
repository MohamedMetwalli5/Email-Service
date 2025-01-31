package com.backendemailservice.backendemailservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.backendemailservice.backendemailservice.entity.User;
import com.backendemailservice.backendemailservice.service.UserService;
import com.backendemailservice.backendemailservice.util.JwtUtil;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
public class OAuth2Controller {

	@Value("${cors.allowed.origin}")
    private String allowedOrigin;
	
	@Value("${server.port}")
    private String serverPort;
	
    @Value("${discord.client.id}")
    private String discordClientId;

    @Value("${discord.client.secret}")
    private String discordClientSecret;

    @Value("${discord.token.url}")
    private String discordTokenUrl;

    @Value("${discord.api.url}")
    private String discordApiUrl;

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Autowired
    public OAuth2Controller(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/DiscordSignin")
    public ResponseEntity<String> handleDiscordLogin(
            @RequestParam("code") String code,
            @RequestParam("state") String state) {

        // Exchanging the authorization code for an access token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("client_id", discordClientId);
        requestBody.add("client_secret", discordClientSecret);
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("code", code);
        requestBody.add("redirect_uri", "http://localhost:"+serverPort+"/DiscordSignin");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> tokenResponse = restTemplate.exchange(
                discordTokenUrl, HttpMethod.POST, requestEntity, Map.class
        );

        if (tokenResponse.getStatusCode().is2xxSuccessful()) {
            String accessToken = (String) tokenResponse.getBody().get("access_token");

            // Using the access token to get user info
            headers.clear();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> userRequestEntity = new HttpEntity<>(headers);

            ResponseEntity<Map> userResponse = restTemplate.exchange(
                    discordApiUrl + "/users/@me", HttpMethod.GET, userRequestEntity, Map.class
            );

            if (userResponse.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> userInfo = userResponse.getBody();
                String email = (String) userInfo.get("email");
                String username = (String) userInfo.get("username");
                
                // Generating the JWT from the fetched email
                String jwtToken = jwtUtil.generateToken(email);
                
                // Creating a redirect URL to the frontend with the JWT token and email
                String redirectUrl = allowedOrigin + "/home?token=" + jwtToken + "&email=" + email;
                
                // Creating a user in the database if it is the first time for them to sign in using Discord
                Optional<User> existingUser = userService.foundReceiver(email);
                if (!existingUser.isPresent()) {
                    String uniqueId = UUID.randomUUID().toString();
                    User user = new User(email, "SignedinWithDiscord" + uniqueId);
                    userService.createUser(user);
                }
                
                return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, redirectUrl).build();
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Failed to fetch user info from Discord.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Failed to get access token from Discord.");
        }
    }
}
    