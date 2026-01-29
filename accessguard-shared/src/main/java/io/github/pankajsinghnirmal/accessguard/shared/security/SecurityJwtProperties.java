package io.github.pankajsinghnirmal.accessguard.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record SecurityJwtProperties(
        String issuer,
        String publicKeyPath,
        String tenantClaim,
        String rolesClaim,
        String rolePrefix,
        boolean requireTenant
) {}