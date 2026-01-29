package io.github.pankajsinghnirmal.accessguard.audit.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "security.jwt.issuer=accessguard-dev",
        "security.jwt.public-key-path=../dev-keys/dev-public.pem",
        "security.jwt.tenant-claim=tenant_id",
        "security.jwt.roles-claim=roles",
        "security.jwt.role-prefix=ROLE_",
        "security.jwt.require-tenant=true"
})
@AutoConfigureMockMvc
class JwtClaimsExtractionIT {

    private static final String ISSUER = "accessguard-dev";
    private static final Path PRIVATE_KEY_PATH = Path.of("../dev-keys/dev-private.pem");
    private static final Path PUBLIC_KEY_PATH = Path.of("../dev-keys/dev-public.pem");

    @Autowired
    private MockMvc mvc;

    @Test
    void shouldRejectRequestWhenTokenIsMissingTenantClaim() throws Exception {
        String token = TokenFactory.mintToken(
                ISSUER,
                null,
                List.of("ADMIN")
        );

        mvc.perform(get("/_test/whoami")
                   .header("Authorization", "Bearer " + token))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRequestWhenTokenHasBlankTenantClaim() throws Exception {
        String token = TokenFactory.mintToken(
                ISSUER,
                "   ",
                List.of("ADMIN")
        );

        mvc.perform(get("/_test/whoami")
                   .header("Authorization", "Bearer " + token))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldExposeTenantAndRoleAuthoritiesWhenTokenContainsTenantAndRoles() throws Exception {
        String token = TokenFactory.mintToken(
                ISSUER,
                "tenant-a",
                List.of("ADMIN", "INTERNAL")
        );

        mvc.perform(get("/secure/test/whoami")
                   .header("Authorization", "Bearer " + token))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.tenant").value("tenant-a"))
           .andExpect(jsonPath("$.authorities", Matchers.hasItem("ROLE_ADMIN")))
           .andExpect(jsonPath("$.authorities", Matchers.hasItem("ROLE_INTERNAL")));
    }

    private static final class TokenFactory {

        static String mintToken(String issuer, String tenantId, List<String> roles) {
            JwtEncoder encoder = jwtEncoder();

            Instant now = Instant.now();
            JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                                                      .issuer(issuer)
                                                      .subject("user-1")
                                                      .issuedAt(now)
                                                      .expiresAt(now.plusSeconds(600))
                                                      .claim("roles", roles);

            if (tenantId != null) {
                claims.claim("tenant_id", tenantId);
            }

            return encoder.encode(JwtEncoderParameters.from(claims.build())).getTokenValue();
        }

        private static JwtEncoder jwtEncoder() {
            RSAPublicKey publicKey = PemKeys.readPublicKey(PUBLIC_KEY_PATH);
            RSAPrivateKey privateKey = PemKeys.readPrivateKey(PRIVATE_KEY_PATH);

            RSAKey rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID("dev")
                    .build();

            JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
            return new NimbusJwtEncoder(jwkSource);
        }
    }

    private static final class PemKeys {

        static RSAPublicKey readPublicKey(Path pemPath) {
            try {
                byte[] der = pemToDer(pemPath, "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");
                X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
                return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to read RSA public key: " + pemPath, e);
            }
        }

        static RSAPrivateKey readPrivateKey(Path pemPath) {
            try {
                byte[] der = pemToDer(pemPath, "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
                return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to read RSA private key: " + pemPath, e);
            }
        }

        private static byte[] pemToDer(Path path, String header, String footer) throws Exception {
            String pem = Files.readString(path)
                              .replace(header, "")
                              .replace(footer, "")
                              .replaceAll("\\s", "");
            return Base64.getDecoder().decode(pem);
        }
    }
}