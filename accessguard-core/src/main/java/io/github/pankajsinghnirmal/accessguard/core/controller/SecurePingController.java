package io.github.pankajsinghnirmal.accessguard.core.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
class SecurePingController {

    @GetMapping("/secure/ping")
    String ping() {
        return "ok";
    }

    @GetMapping("/secure/whoami")
    Map<String, Object> whoami(Authentication authentication) {
        var authorities = authentication.getAuthorities().stream()
                                        .map(GrantedAuthority::getAuthority)
                                        .sorted()
                                        .collect(Collectors.toList());

        return Map.of(
                "principalType", authentication.getPrincipal().getClass().getName(),
                "name", authentication.getName(),
                "authorities", authorities
        );
    }

    @GetMapping("/secure/admin")
    String admin() {
        return "admin-ok";
    }
}