package io.github.pankajsinghnirmal.accessguard.core.security;

import io.github.pankajsinghnirmal.accessguard.shared.security.RolesClaimAuthoritiesConverter;
import io.github.pankajsinghnirmal.accessguard.shared.security.SecurityJwtProperties;
import io.github.pankajsinghnirmal.accessguard.shared.security.StaticKeyJwtDecoders;
import io.github.pankajsinghnirmal.accessguard.shared.security.TenantClaimValidator;
import io.github.pankajsinghnirmal.accessguard.shared.tenant.TenantContextFilter;
import io.github.pankajsinghnirmal.accessguard.shared.trace.CorrelationIdFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.LinkedHashSet;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableConfigurationProperties(SecurityJwtProperties.class)
public class SecurityConfig {

    @Bean
    TenantContextFilter tenantContextFilter(SecurityJwtProperties props) {
        return new TenantContextFilter(props);
    }

    @Bean
    CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthConverter,
            TenantContextFilter tenantContextFilter,
            CorrelationIdFilter correlationIdFilter
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/secure/admin/**").hasRole("ADMIN")
                        .requestMatchers("/internal/**").hasRole("INTERNAL")
                        .requestMatchers("/secure/**").authenticated()
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthConverter)
                        )
                )
                .addFilterBefore(correlationIdFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(tenantContextFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecurityJwtProperties props) {
        NimbusJwtDecoder decoder = StaticKeyJwtDecoders.fromPublicKeyPath(props.publicKeyPath());

        OAuth2TokenValidator<Jwt> base = JwtValidators.createDefaultWithIssuer(props.issuer());
        OAuth2TokenValidator<Jwt> tenant = new TenantClaimValidator(props.tenantClaim(), props.requireTenant());

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(base, tenant));
        return decoder;
    }

    @Bean
    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter(SecurityJwtProperties props) {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        RolesClaimAuthoritiesConverter rolesConverter =
                new RolesClaimAuthoritiesConverter(props.rolesClaim(), props.rolePrefix());

        return jwt -> {
            var authorities = new LinkedHashSet<GrantedAuthority>();
            authorities.addAll(scopeConverter.convert(jwt));
            authorities.addAll(rolesConverter.convert(jwt));
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }
}