package io.github.pankajsinghnirmal.accessguard.core.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
class TestWhoamiController {

    @GetMapping("/secure/test/whoami")
    Map<String, Object> whoami(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        String tenant = jwt.getClaimAsString("tenant_id");

        var authorities = authentication.getAuthorities().stream()
                                        .map(GrantedAuthority::getAuthority)
                                        .sorted()
                                        .toList();

        String principalType = (authentication.getPrincipal() != null)
                ? authentication.getPrincipal().getClass().getName()
                : null;
        String subject = (authentication instanceof JwtAuthenticationToken jat) ? jat.getToken().getSubject() : null;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tenant", tenant);
        body.put("authorities", authorities);

        if (principalType != null) body.put("principalType", principalType);
        if (subject != null) body.put("subject", subject);

        return body;
    }
}