package io.github.pankajsinghnirmal.accessguard.core.propagation;

import io.github.pankajsinghnirmal.accessguard.core.client.audit.AuditFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "testAuditInternalClient",
        url = "${audit.test.base-url}",
        configuration = AuditFeignConfig.class
)
interface TestAuditInternalClient {

    @GetMapping("/internal/ping")
    String pingInternal();
}