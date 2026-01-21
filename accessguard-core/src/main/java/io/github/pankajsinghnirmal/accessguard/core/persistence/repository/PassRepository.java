package io.github.pankajsinghnirmal.accessguard.core.persistence.repository;

import io.github.pankajsinghnirmal.accessguard.core.persistence.entity.PassEntity;
import io.github.pankajsinghnirmal.accessguard.core.persistence.entity.PassStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PassRepository extends JpaRepository<PassEntity, UUID> {

    Optional<PassEntity> findByTenantIdAndId(String tenantId, UUID id);

    Optional<PassEntity> findByTenantIdAndPassCode(String tenantId, String passCode);

    boolean existsByTenantIdAndPassCode(String tenantId, String passCode);

    long countByTenantIdAndStatus(String tenantId, PassStatus status);

    long countByTenantIdAndValidToBefore(String tenantId, Instant timestamp);
}