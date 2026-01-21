package io.github.pankajsinghnirmal.accessguard.audit.persistence.entity;

import io.github.pankajsinghnirmal.accessguard.audit.persistence.repository.AuditAccessAttemptRepository;
import io.github.pankajsinghnirmal.accessguard.audit.persistence.repository.AuditEventRepository;
import io.github.pankajsinghnirmal.accessguard.shared.contracts.AccessDecision;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuditPersistenceSmokeIT {

    @Autowired
    private Flyway flyway;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AuditAccessAttemptRepository auditAccessAttemptRepository;

    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("accessguard_core")
                    .withUsername("accessguard")
                    .withPassword("accessguard");

    @Test
    void shouldApplyMigrationsAndPersistAuditAccessAttemptWhenDatabaseIsRunning() {
        // Flyway ran (at least one migration applied)
        assertThat(flyway.info().applied())
                .isNotNull()
                .isNotEmpty();

        // Persist event
        UUID eventId = UUID.randomUUID();
        String tenantId = "TENANT-123";
        Instant occurredAt = Instant.parse("2026-01-11T10:05:12Z");
        String correlationId = "b8c7d0c1-2a63-4a9b-bd0c-0e9e3e3b0b2a";

        AuditEventEntity event = new AuditEventEntity();
        event.setEventId(eventId);
        event.setTenantId(tenantId);
        event.setEventType("accessguard.access_attempt_recorded");
        event.setSchemaVersion(1);
        event.setOccurredAt(occurredAt);
        event.setCorrelationId(correlationId);

        auditEventRepository.saveAndFlush(event);

        // Persist audit access attempt
        UUID attemptId = UUID.fromString("a3c8a6e1-1f4d-4f5b-9e0c-61d9c0a1b123");

        AuditAccessAttemptEntity attempt = new AuditAccessAttemptEntity();
        attempt.setId(UUID.randomUUID());
        attempt.setTenantId(tenantId);
        attempt.setEvent(event);
        attempt.setAttemptId(attemptId);
        attempt.setDoorCode("DOOR-A1");
        attempt.setZoneCode("ZONE-BLDG-A-F3");
        attempt.setPassRef("7c91e1b2-4d3a-42c3-9e91-abc123abc123");
        attempt.setDecision(AccessDecision.GRANTED);
        attempt.setReasonCode("OK");
        attempt.setOccurredAt(occurredAt);
        attempt.setEvaluatedAt(occurredAt);
        attempt.setCorrelationId(correlationId);

        auditAccessAttemptRepository.saveAndFlush(attempt);

        // Read back and assert key fields
        AuditAccessAttemptEntity loaded = auditAccessAttemptRepository
                .findByTenantIdAndAttemptId(tenantId, attemptId)
                .orElseThrow();

        assertThat(loaded.getTenantId()).isEqualTo(tenantId);
        assertThat(loaded.getAttemptId()).isEqualTo(attemptId);
        assertThat(loaded.getDecision()).isEqualTo(AccessDecision.GRANTED);
        assertThat(loaded.getReasonCode()).isEqualTo("OK");
        assertThat(loaded.getEvent().getEventId()).isEqualTo(eventId);
    }
}