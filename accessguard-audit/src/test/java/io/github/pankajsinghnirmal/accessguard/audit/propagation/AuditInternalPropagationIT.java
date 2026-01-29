package io.github.pankajsinghnirmal.accessguard.audit.propagation;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestInternalEchoController.class)
class AuditInternalPropagationIT {

    private static final String ISSUER = "accessguard-dev";

    @Autowired
    private MockMvc mvc;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        Path publicKey = findUpwards("dev-keys/dev-public.pem");
        registry.add("security.jwt.issuer", () -> ISSUER);
        registry.add("security.jwt.public-key-path", publicKey::toString);
        registry.add("security.jwt.require-tenant", () -> "true");
        registry.add("security.jwt.tenant-claim", () -> "tenant_id");
        registry.add("security.jwt.roles-claim", () -> "roles");
        registry.add("security.jwt.role-prefix", () -> "ROLE_");
    }

    @Test
    void shouldReturn200AndExposeTenantAndCorrelationIdWhenInternalEndpointIsCalledWithValidInternalJwt() throws Exception {
        String token = mintToken("tenant-a", List.of("INTERNAL"));

        mvc.perform(get("/internal/echo")
                   .header("Authorization", "Bearer " + token)
                   .header("X-Correlation-Id", "corr-999"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.tenantId").value("tenant-a"))
           .andExpect(jsonPath("$.correlationId").value("corr-999"));
    }

    @Test
    void shouldReturn403WhenInternalEndpointIsCalledWithoutInternalRole() throws Exception {
        String token = mintToken("tenant-a", List.of("USER"));

        mvc.perform(get("/internal/echo")
                   .header("Authorization", "Bearer " + token)
                   .header("X-Correlation-Id", "corr-999"))
           .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401WhenInternalEndpointIsCalledWithTokenMissingTenantClaim() throws Exception {
        String token = mintToken(null, List.of("INTERNAL"));

        mvc.perform(get("/internal/echo")
                   .header("Authorization", "Bearer " + token)
                   .header("X-Correlation-Id", "corr-999"))
           .andExpect(status().isUnauthorized());
    }

    private static String mintToken(String tenantId, List<String> roles) {
        JwtEncoder encoder = jwtEncoder();
        Instant now = Instant.now();

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                                                  .issuer(ISSUER)
                                                  .subject("accessguard-core")
                                                  .issuedAt(now)
                                                  .expiresAt(now.plusSeconds(120))
                                                  .claim("roles", roles);

        if (tenantId != null) {
            claims.claim("tenant_id", tenantId);
        }

        return encoder.encode(JwtEncoderParameters.from(claims.build())).getTokenValue();
    }

    private static JwtEncoder jwtEncoder() {
        Path publicKeyPath = findUpwards("dev-keys/dev-public.pem");
        Path privateKeyPath = findUpwards("dev-keys/dev-private.pem");

        RSAPublicKey publicKey = readPublicKey(publicKeyPath);
        RSAPrivateKey privateKey = readPrivateKey(privateKeyPath);

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("dev")
                .build();

        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwkSource);
    }

    private static RSAPublicKey readPublicKey(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return RsaKeyConverters.x509().convert(in);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read RSA public key: " + path, e);
        }
    }

    private static RSAPrivateKey readPrivateKey(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return RsaKeyConverters.pkcs8().convert(in);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read RSA private key: " + path, e);
        }
    }

    private static Path findUpwards(String relative) {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 8; i++) {
            Path candidate = dir.resolve(relative);
            if (Files.exists(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
            if (dir == null) break;
        }
        throw new IllegalStateException("File not found: " + relative);
    }
}