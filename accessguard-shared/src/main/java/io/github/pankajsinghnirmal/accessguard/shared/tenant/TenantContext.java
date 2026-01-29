package io.github.pankajsinghnirmal.accessguard.shared.tenant;

import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

public interface TenantContext {

    Optional<String> extractTenantId();

    default String requireTenantId() {
        return extractTenantId().orElseThrow(() -> new AccessDeniedException("Missing tenant context"));
    }
}