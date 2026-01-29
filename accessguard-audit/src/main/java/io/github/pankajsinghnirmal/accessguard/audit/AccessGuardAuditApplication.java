package io.github.pankajsinghnirmal.accessguard.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "io.github.pankajsinghnirmal.accessguard.audit",
        "io.github.pankajsinghnirmal.accessguard.shared"
})
public class AccessGuardAuditApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccessGuardAuditApplication.class, args);
    }
}
