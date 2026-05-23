package com.backendemailservice.backendemailservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    // short-lived access token — 30 minutes
    private static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 30;

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Deprecated
    public String generateToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", List.of("ROLE_USER"));
        return generateToken(email, claims);
    }

    public String generateToken(String email, Map<String, Object> extraClaims) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String email) {
        try {
            String extractedEmail = extractEmail(token);
            return email.equals(extractedEmail) && !isTokenExpired(token);
        } catch (ExpiredJwtException ex) {
            log.info("JWT expired for email {}: {}", email, ex.getMessage());
            return false;
        } catch (Exception ex) {
            log.warn("JWT validation failed for email {}: {}", email, ex.getMessage(), ex);
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    private Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or malformed token");
        }
    }

    public List<String> extractRoles(String token) {
        Claims claims = extractClaims(token);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?> roleList) {
            return roleList.stream()
                    .filter(r -> r instanceof String)
                    .map(r -> (String) r)
                    .toList();
        }
        return List.of("ROLE_USER");
    }

    public String extractAndValidateToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authorizationHeader.substring(7);
        String email = extractEmail(token);
        if (!isTokenValid(token, email)) {
            return null;
        }
        return email;
    }
}
