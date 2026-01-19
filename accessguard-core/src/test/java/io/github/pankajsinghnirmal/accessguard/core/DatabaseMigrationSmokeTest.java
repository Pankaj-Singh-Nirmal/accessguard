package io.github.pankajsinghnirmal.accessguard.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ExtendWith(SpringExtension.class)
class DatabaseMigrationSmokeTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("accessguard_core")
                    .withUsername("accessguard")
                    .withPassword("accessguard");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Ensure Flyway uses the same container-backed database.
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @Autowired
    Flyway flyway;

    @Test
    void shouldStartContextWithFlywayMigrationsApplied() {
        var info = flyway.info();

        assertThat(info.applied())
                .as("Expected at least one Flyway migration to be applied")
                .isNotEmpty();

        assertThat(info.current())
                .as("Expected a current applied migration")
                .isNotNull();

        assertThat(info.current().getVersion())
                .as("Expected a versioned migration")
                .isNotNull();
    }
}
