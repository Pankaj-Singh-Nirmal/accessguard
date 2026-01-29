package io.github.pankajsinghnirmal.accessguard.audit.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SecurePingController {

    @GetMapping("/_secure/ping")
    String ping() {
        return "ok";
    }
}