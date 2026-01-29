package io.github.pankajsinghnirmal.accessguard.core.propagation;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@EnableFeignClients(clients = TestAuditInternalClient.class)
class CoreToAuditPropagationIT {

    private static final String ISSUER = "accessguard-dev";

    private static HttpServer server;
    private static int port;

    private static final AtomicReference<String> lastAuthorization = new AtomicReference<>();
    private static final AtomicReference<String> lastCorrelationId = new AtomicReference<>();

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @BeforeAll
    static void startAuditStubServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();

        server.createContext("/internal/ping", exchange -> {
            record(exchange);

            byte[] body = "ok".getBytes(UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        server.start();
    }

    @AfterAll
    static void stopAuditStubServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("audit.test.base-url", () -> "http://localhost:" + port);
    }

    @Test
    void shouldPropagateCorrelationIdAndInternalJwtWhenTriggerEndpointCallsAuditInternalEndpoint() throws Exception {
        lastAuthorization.set(null);
        lastCorrelationId.set(null);

        mvc.perform(get("/secure/test/trigger-audit")
                   .header("X-Correlation-Id", "corr-123")
                   .with(jwt().jwt(j -> j.claim("tenantId", "tenant-a"))
                              .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
           .andExpect(status().isOk());

        Assertions.assertThat(lastCorrelationId.get()).isEqualTo("corr-123");

        String authHeader = lastAuthorization.get();
        Assertions.assertThat(authHeader).isNotBlank();
        Assertions.assertThat(authHeader).startsWith("Bearer ");

        String token = authHeader.substring("Bearer ".length());
        Jwt decoded = jwtDecoder.decode(token);

        Assertions.assertThat(decoded.getClaimAsString("iss")).isEqualTo(ISSUER);
        Assertions.assertThat(decoded.getClaimAsString("tenantId")).isEqualTo("tenant-a");
        Assertions.assertThat(decoded.getClaimAsStringList("roles")).contains("INTERNAL");
    }

    @Test
    void shouldGenerateCorrelationIdAndPropagateItWhenInboundRequestDoesNotProvideOne() throws Exception {
        lastAuthorization.set(null);
        lastCorrelationId.set(null);

        MvcResult result = mvc.perform(get("/secure/test/trigger-audit")
                                      .with(jwt().jwt(j -> j.claim("tenantId", "tenant-a"))
                                                 .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                              .andExpect(status().isOk())
                              .andReturn();

        String responseCorrelationId = result.getResponse().getHeader("X-Correlation-Id");
        Assertions.assertThat(responseCorrelationId).isNotBlank();
        Assertions.assertThat(lastCorrelationId.get()).isEqualTo(responseCorrelationId);
    }

    private static void record(HttpExchange exchange) {
        List<String> auth = exchange.getRequestHeaders().get("Authorization");
        List<String> corr = exchange.getRequestHeaders().get("X-Correlation-Id");

        lastAuthorization.set((auth == null || auth.isEmpty()) ? null : auth.getFirst());
        lastCorrelationId.set((corr == null || corr.isEmpty()) ? null : corr.getFirst());
    }

    @TestConfiguration
    static class TestConfig {

        @RestController
        static class TestAuditTriggerController {

            private final TestAuditInternalClient audit;

            TestAuditTriggerController(TestAuditInternalClient audit) {
                this.audit = audit;
            }

            @GetMapping("/secure/test/trigger-audit")
            String triggerAuditInternalPing() {
                return audit.pingInternal();
            }
        }
    }
}