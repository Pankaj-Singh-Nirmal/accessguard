package io.github.pankajsinghnirmal.accessguard.core.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
public final class InternalServiceJwtProvider {

    private final JwtEncoder encoder;
    private final String issuer;
    private final String subject;

    public InternalServiceJwtProvider(
            @Value("${security.internal-jwt.private-key}") Resource privateKey,
            @Value("${security.jwt.public-key}") Resource publicKey,
            @Value("${security.internal-jwt.issuer}") String issuer,
            @Value("${security.internal-jwt.subject}") String subject
    ) {
        this.encoder = buildEncoder(publicKey, privateKey);
        this.issuer = issuer;
        this.subject = subject;
    }

    public String mintInternalToken(String tenantId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                                          .issuer(issuer)
                                          .subject(subject)
                                          .id(UUID.randomUUID().toString())
                                          .issuedAt(now)
                                          .expiresAt(now.plusSeconds(120))
                                          .claim("tenant_id", tenantId)
                                          .claim("roles", List.of("INTERNAL"))
                                          .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    private static JwtEncoder buildEncoder(Resource publicKey, Resource privateKey) {
        RSAPublicKey pub = readPublicKey(publicKey);
        RSAPrivateKey priv = readPrivateKey(privateKey);

        RSAKey rsaKey = new RSAKey.Builder(pub)
                .privateKey(priv)
                .keyID("dev")
                .build();

        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwkSource);
    }

    private static RSAPublicKey readPublicKey(Resource resource) {
        try {
            String pem = readPem(resource)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] der = Base64.getDecoder().decode(pem);
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                                            .generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA public key from " + resource.getDescription(), e);
        }
    }

    private static RSAPrivateKey readPrivateKey(Resource resource) {
        try {
            String pem = readPem(resource)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] der = Base64.getDecoder().decode(pem);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                                             .generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA private key from " + resource.getDescription(), e);
        }
    }

    private static String readPem(Resource resource) throws Exception {
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}