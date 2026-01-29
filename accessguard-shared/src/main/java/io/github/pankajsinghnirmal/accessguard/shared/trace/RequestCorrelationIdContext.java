package io.github.pankajsinghnirmal.accessguard.shared.trace;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

@Component
public final class RequestCorrelationIdContext implements CorrelationIdContext {

    public static final String REQUEST_ATTRIBUTE_CORRELATION_ID = "accessguard.correlationId";

    @Override
    public Optional<String> correlationId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.empty();
        }
        Object raw = attributes.getAttribute(REQUEST_ATTRIBUTE_CORRELATION_ID, RequestAttributes.SCOPE_REQUEST);
        if (raw instanceof String id && !id.isBlank()) {
            return Optional.of(id);
        }
        return Optional.empty();
    }
}