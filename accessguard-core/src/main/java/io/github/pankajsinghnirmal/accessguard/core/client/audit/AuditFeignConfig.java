package io.github.pankajsinghnirmal.accessguard.core.client.audit;

import feign.RequestInterceptor;
import io.github.pankajsinghnirmal.accessguard.core.security.InternalServiceJwtProvider;
import io.github.pankajsinghnirmal.accessguard.shared.http.RequestHeaders;
import io.github.pankajsinghnirmal.accessguard.shared.tenant.TenantContext;
import io.github.pankajsinghnirmal.accessguard.shared.trace.CorrelationIdContext;
import org.springframework.context.annotation.Bean;

public class AuditFeignConfig {

    @Bean
    RequestInterceptor auditRequestPropagationInterceptor(
            TenantContext tenantContext,
            CorrelationIdContext correlationIdContext,
            InternalServiceJwtProvider internalServiceJwtProvider
    ) {
        return template -> {
            String tenantId = tenantContext.requireTenantId();
            String correlationId = correlationIdContext.requireCorrelationId();
            String token = internalServiceJwtProvider.mintInternalToken(tenantId);

            template.header("Authorization", "Bearer " + token);
            template.header(RequestHeaders.CORRELATION_ID, correlationId);
        };
    }
}