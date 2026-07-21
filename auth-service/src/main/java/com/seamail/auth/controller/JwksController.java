package com.seamail.auth.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.seamail.auth.config.RsaKeyProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Public JWKS endpoint. Resource servers fetch the public key set from here
// to validate RS256 access tokens statelessly.
@RestController
public class JwksController {

    private final RsaKeyProvider keyProvider;

    public JwksController(RsaKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        RSAKey jwk = new RSAKey.Builder(keyProvider.getPublicKey())
                .keyID(keyProvider.getKeyId())
                .build();
        return new JWKSet(jwk).toJSONObject();
    }
}
