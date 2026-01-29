package io.github.pankajsinghnirmal.accessguard.shared.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class RolesClaimAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String rolesClaim;
    private final String rolePrefix;

    public RolesClaimAuthoritiesConverter(String rolesClaim, String rolePrefix) {
        this.rolesClaim = Objects.requireNonNull(rolesClaim);
        this.rolePrefix = Objects.requireNonNull(rolePrefix);
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object raw = jwt.getClaims().get(rolesClaim);
        if (raw == null) {
            return List.of();
        }

        Collection<String> roles = switch (raw) {
            case Collection<?> c -> c.stream().map(String::valueOf).toList();
            case String s -> Arrays.stream(s.split("\\s+")).filter(x -> !x.isBlank()).toList();
            default -> List.of(String.valueOf(raw));
        };

        return roles.stream()
                    .map(String::trim)
                    .filter(r -> !r.isBlank())
                    .map(r -> r.startsWith(rolePrefix) ? r : rolePrefix + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}