package io.github.pankajsinghnirmal.accessguard.core.persistence.repository;

import io.github.pankajsinghnirmal.accessguard.core.persistence.entity.ZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZoneRepository extends JpaRepository<ZoneEntity, UUID> {

    Optional<ZoneEntity> findByTenantIdAndId(String tenantId, UUID id);

    Optional<ZoneEntity> findByTenantIdAndZoneCode(String tenantId, String zoneCode);

    List<ZoneEntity> findByTenantIdAndParentZone_Id(String tenantId, UUID parentZoneId);
}