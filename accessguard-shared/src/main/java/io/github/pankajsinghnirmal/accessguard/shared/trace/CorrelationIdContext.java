package io.github.pankajsinghnirmal.accessguard.shared.trace;

import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

public interface CorrelationIdContext {

    Optional<String> correlationId();

    default String requireCorrelationId() {
        return correlationId().orElseThrow(() -> new AccessDeniedException("Missing correlation id"));
    }
}