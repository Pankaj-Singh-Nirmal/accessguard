package io.github.pankajsinghnirmal.accessguard.core.persistence.entity;

import io.github.pankajsinghnirmal.accessguard.core.persistence.repository.ZoneRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CorePersistenceSmokeIT {

    @Autowired
    private Flyway flyway;

    @Autowired
    private ZoneRepository zoneRepository;

    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("accessguard_core")
                    .withUsername("accessguard")
                    .withPassword("accessguard");

    @Test
    void shouldApplyMigrationsAndPersistZoneWhenDatabaseIsRunning() {
        // Flyway ran (at least one migration applied)
        assertThat(flyway.info().applied())
                .isNotNull()
                .isNotEmpty();

        // Persist zone
        UUID id = UUID.randomUUID();

        ZoneEntity zone = new ZoneEntity();
        zone.setId(id);
        zone.setTenantId("TENANT-123");
        zone.setZoneCode("ZONE-BLDG-A-F3");
        zone.setName("Building A - Floor 3");
        zone.setStatus(ZoneStatus.ACTIVE);

        zoneRepository.saveAndFlush(zone);

        ZoneEntity loaded = zoneRepository
                .findByTenantIdAndZoneCode("TENANT-123", "ZONE-BLDG-A-F3")
                .orElseThrow();

        assertThat(loaded.getId()).isEqualTo(id);
        assertThat(loaded.getTenantId()).isEqualTo("TENANT-123");
        assertThat(loaded.getZoneCode()).isEqualTo("ZONE-BLDG-A-F3");
        assertThat(loaded.getStatus()).isEqualTo(ZoneStatus.ACTIVE);
        assertThat(loaded.getName()).isEqualTo("Building A - Floor 3");
    }
}