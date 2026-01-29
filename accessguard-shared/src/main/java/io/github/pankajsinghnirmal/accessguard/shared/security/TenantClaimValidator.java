package io.github.pankajsinghnirmal.accessguard.shared.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

public final class TenantClaimValidator implements OAuth2TokenValidator<Jwt> {

    private final String claimName;
    private final boolean required;
    private static final String INVALID_TOKEN = "invalid_token";

    public TenantClaimValidator(String claimName, boolean required) {
        this.claimName = Objects.requireNonNull(claimName);
        this.required = required;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (!required) return OAuth2TokenValidatorResult.success();

        Object raw = jwt.getClaims().get(claimName);
        if (!(raw instanceof String tenant) || tenant.isBlank()) {
            return failure("Missing or blank tenant claim: " + claimName);
        }
        if (tenant.length() > 64) {
            return failure("Tenant claim too long: " + claimName);
        }
        return OAuth2TokenValidatorResult.success();
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(INVALID_TOKEN, description, null));
    }
}