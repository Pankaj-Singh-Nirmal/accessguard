package io.github.pankajsinghnirmal.accessguard.audit.persistence.repository;

import io.github.pankajsinghnirmal.accessguard.audit.persistence.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

    Optional<AuditEventEntity> findByTenantIdAndEventId(String tenantId, UUID eventId);

    boolean existsByTenantIdAndEventId(String tenantId, UUID eventId);
}