package io.github.pankajsinghnirmal.accessguard.core.context;

import io.github.pankajsinghnirmal.accessguard.shared.tenant.RequestTenantContext;
import io.github.pankajsinghnirmal.accessguard.shared.tenant.TenantContext;
import io.github.pankajsinghnirmal.accessguard.shared.trace.CorrelationIdContext;
import io.github.pankajsinghnirmal.accessguard.shared.trace.RequestCorrelationIdContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestContextConfig {

    @Bean
    TenantContext tenantContext() {
        return new RequestTenantContext();
    }

    @Bean
    CorrelationIdContext correlationIdContext() {
        return new RequestCorrelationIdContext();
    }
}