package io.github.pankajsinghnirmal.accessguard.audit.persistence.repository;

import io.github.pankajsinghnirmal.accessguard.audit.persistence.entity.AuditAccessAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuditAccessAttemptRepository extends JpaRepository<AuditAccessAttemptEntity, UUID> {

    Optional<AuditAccessAttemptEntity> findByTenantIdAndAttemptId(String tenantId, UUID attemptId);

    boolean existsByTenantIdAndAttemptId(String tenantId, UUID attemptId);
}