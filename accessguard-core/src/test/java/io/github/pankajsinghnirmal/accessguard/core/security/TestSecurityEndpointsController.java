package io.github.pankajsinghnirmal.accessguard.core.security;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TestSecurityEndpointsController {

    @GetMapping("/secure/test/ping")
    String securePing() {
        return "ok";
    }

    @GetMapping("/secure/admin/test/ping")
    String adminPing() {
        return "admin-ok";
    }

    @GetMapping("/internal/test/ping")
    String internalPing() {
        return "internal-ok";
    }
}