package com.backendemailservice.backendemailservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

	@Value("${jwt.secret}")
    private String SECRET_KEY;

    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 10; // 10 hours

    // Generating a token for the given email
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    // Extracting email from the token
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    // Validating the token by checking its email and expiration status
    public boolean isTokenValid(String token, String email) {
        try {
            return email.equals(extractEmail(token)) && !isTokenExpired(token);
        } catch (Exception e) {
            return false; // If there is an exception (like invalid token), we return false
        }
    }

    // Checking if the token has expired
    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    // Extracting claims from the token
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
}
