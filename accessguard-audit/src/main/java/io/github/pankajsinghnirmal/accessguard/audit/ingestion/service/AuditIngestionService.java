package io.github.pankajsinghnirmal.accessguard.audit.ingestion.service;

import io.github.pankajsinghnirmal.accessguard.shared.tenant.TenantContext;
import io.github.pankajsinghnirmal.accessguard.shared.trace.CorrelationIdContext;
import org.springframework.stereotype.Service;

@Service
class AuditIngestionService {

    private final TenantContext tenantContext;
    private final CorrelationIdContext correlationIdContext;

    AuditIngestionService(TenantContext tenantContext, CorrelationIdContext correlationIdContext) {
        this.tenantContext = tenantContext;
        this.correlationIdContext = correlationIdContext;
    }

    void persistSomethingFromCore(/* payload */) {
        String tenantId = tenantContext.requireTenantId();
        String correlationId = correlationIdContext.requireCorrelationId();

        // persistence with tenantId + correlationId columns in the upcoming stages
    }
}