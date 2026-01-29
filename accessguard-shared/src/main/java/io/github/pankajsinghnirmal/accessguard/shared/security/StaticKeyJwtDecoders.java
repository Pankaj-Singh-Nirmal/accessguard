package io.github.pankajsinghnirmal.accessguard.shared.security;

import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class StaticKeyJwtDecoders {

    private StaticKeyJwtDecoders() {}

    public static NimbusJwtDecoder fromPublicKeyPath(String publicKeyPath) {
        RSAPublicKey publicKey = readRsaPublicKey(publicKeyPath);
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    private static RSAPublicKey readRsaPublicKey(String path) {
        try {
            String pem = Files.readString(Path.of(path))
                              .replace("-----BEGIN PUBLIC KEY-----", "")
                              .replace("-----END PUBLIC KEY-----", "")
                              .replaceAll("\\s", "");

            byte[] der = Base64.getDecoder().decode(pem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(der);

            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA public key from: " + path, e);
        }
    }
}