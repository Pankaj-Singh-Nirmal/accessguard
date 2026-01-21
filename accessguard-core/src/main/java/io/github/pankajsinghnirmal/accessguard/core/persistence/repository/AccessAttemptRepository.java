package io.github.pankajsinghnirmal.accessguard.core.persistence.repository;

import io.github.pankajsinghnirmal.accessguard.core.persistence.entity.AccessAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccessAttemptRepository extends JpaRepository<AccessAttemptEntity, UUID> {

    Optional<AccessAttemptEntity> findByTenantIdAndAttemptId(String tenantId, UUID attemptId);

    boolean existsByTenantIdAndAttemptId(String tenantId, UUID attemptId);
}