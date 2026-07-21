package com.seamail.auth.service;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.seamail.auth.config.RsaKeyProvider;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Issues RS256 access tokens (30 min) and opaque refresh tokens.
// Replaces the old HS256 JwtUtil: resource servers validate via the public JWKS,
// so no shared secret exists anywhere.
@Service
public class JwtService {

    private static final long ACCESS_TOKEN_EXPIRATION_SECONDS = 60 * 30;

    private final JwtEncoder jwtEncoder;

    public JwtService(RsaKeyProvider keyProvider) {
        RSAKey rsaKey = new RSAKey.Builder(keyProvider.getPublicKey())
                .privateKey(keyProvider.getPrivateKey())
                .keyID(keyProvider.getKeyId())
                .build();
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    public String generateToken(String email) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("seamail-auth-service")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ACCESS_TOKEN_EXPIRATION_SECONDS))
                .subject(email)
                .claim("roles", List.of("ROLE_USER"))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }
}
