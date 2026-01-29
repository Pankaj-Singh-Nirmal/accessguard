package io.github.pankajsinghnirmal.accessguard.shared.tenant;

import io.github.pankajsinghnirmal.accessguard.shared.security.SecurityJwtProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public final class TenantContextFilter extends OncePerRequestFilter {

    private final SecurityJwtProperties props;

    public TenantContextFilter(SecurityJwtProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                String tenantClaim = props.tenantClaim();
                String tenantId = jwtAuth.getToken().getClaimAsString(tenantClaim);

                if (tenantId != null && !tenantId.isBlank()) {
                    request.setAttribute(RequestTenantContext.REQUEST_ATTRIBUTE_TENANT_ID, tenantId);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            request.removeAttribute(RequestTenantContext.REQUEST_ATTRIBUTE_TENANT_ID);
        }
    }
}