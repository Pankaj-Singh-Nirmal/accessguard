package io.github.pankajsinghnirmal.accessguard.audit.propagation;

import io.github.pankajsinghnirmal.accessguard.shared.tenant.TenantContext;
import io.github.pankajsinghnirmal.accessguard.shared.trace.CorrelationIdContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
class TestInternalEchoController {

    private final TenantContext tenantContext;
    private final CorrelationIdContext correlationIdContext;

    TestInternalEchoController(TenantContext tenantContext, CorrelationIdContext correlationIdContext) {
        this.tenantContext = tenantContext;
        this.correlationIdContext = correlationIdContext;
    }

    @GetMapping("/internal/echo")
    Map<String, Object> echo() {
        return Map.of(
                "tenantId", tenantContext.requireTenantId(),
                "correlationId", correlationIdContext.requireCorrelationId()
        );
    }
}