package io.github.pankajsinghnirmal.accessguard.core.persistence.repository;

import io.github.pankajsinghnirmal.accessguard.core.persistence.entity.DoorEntity;
import io.github.pankajsinghnirmal.accessguard.core.persistence.entity.DoorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoorRepository extends JpaRepository<DoorEntity, UUID> {

    Optional<DoorEntity> findByTenantIdAndId(String tenantId, UUID id);

    Optional<DoorEntity> findByTenantIdAndDoorCode(String tenantId, String doorCode);

    List<DoorEntity> findByTenantIdAndZone_Id(String tenantId, UUID zoneId);

    List<DoorEntity> findByTenantIdAndStatus(String tenantId, DoorStatus status);
}