package io.github.pankajsinghnirmal.accessguard.shared.tenant;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

@Component
public final class RequestTenantContext implements TenantContext {

    public static final String REQUEST_ATTRIBUTE_TENANT_ID = "accessguard.tenantId";

    @Override
    public Optional<String> extractTenantId() {
        Optional<String> tenantIdFromRequest = extractTenantIdFromRequestAttributes();
        if (tenantIdFromRequest.isPresent()) {
            return tenantIdFromRequest;
        }
        return extractTenantIdFromSecurityContext();
    }

    private static Optional<String> extractTenantIdFromRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.empty();
        }
        Object raw = attributes.getAttribute(REQUEST_ATTRIBUTE_TENANT_ID, RequestAttributes.SCOPE_REQUEST);
        if (raw instanceof String tenant && !tenant.isBlank()) {
            return Optional.of(tenant);
        }
        return Optional.empty();
    }

    private static Optional<String> extractTenantIdFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            String tenant = jwtAuth.getToken().getClaimAsString("tenant_id");
            if (tenant != null && !tenant.isBlank()) {
                return Optional.of(tenant);
            }
        }
        return Optional.empty();
    }
}