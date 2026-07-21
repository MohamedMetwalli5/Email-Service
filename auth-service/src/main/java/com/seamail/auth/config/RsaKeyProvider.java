package com.seamail.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

// Provides the RSA keypair used to sign access tokens.
// Production-like setup loads PEM files via jwt.private-key-path / jwt.public-key-path.
// When unset, an ephemeral pair is generated so local dev needs no key management;
// the trade-off is that all tokens become invalid on restart.
@Component
public class RsaKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyProvider.class);

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;
    private final String keyId;

    public RsaKeyProvider(
            @Value("${jwt.private-key-path:}") String privateKeyPath,
            @Value("${jwt.public-key-path:}") String publicKeyPath) throws Exception {
        if (privateKeyPath != null && !privateKeyPath.isBlank()
                && publicKeyPath != null && !publicKeyPath.isBlank()) {
            this.privateKey = loadPrivateKey(privateKeyPath);
            this.publicKey = loadPublicKey(publicKeyPath);
            this.keyId = UUID.randomUUID().toString();
            log.info("Loaded RSA keypair from configured PEM files (kid={})", keyId);
        } else {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.keyId = UUID.randomUUID().toString();
            log.warn("No RSA key files configured (jwt.private-key-path/jwt.public-key-path). "
                    + "Generated an ephemeral keypair - all tokens become invalid on restart.");
        }
    }

    private RSAPublicKey loadPublicKey(String path) throws Exception {
        String pem = Files.readString(Path.of(path))
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(pem);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    private RSAPrivateKey loadPrivateKey(String path) throws Exception {
        String pem = Files.readString(Path.of(path))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(pem);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public String getKeyId() {
        return keyId;
    }
}
