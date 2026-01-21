package io.github.pankajsinghnirmal.accessguard.core.persistence.repository;

import io.github.pankajsinghnirmal.accessguard.core.persistence.entity.PassScopeZoneEntity;
import io.github.pankajsinghnirmal.accessguard.core.persistence.entity.PassScopeZoneId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PassScopeZoneRepository extends JpaRepository<PassScopeZoneEntity, PassScopeZoneId> {

    List<PassScopeZoneEntity> findByTenantIdAndIdPassId(String tenantId, UUID passId);

    boolean existsByTenantIdAndIdPassIdAndIdZoneId(String tenantId, UUID passId, UUID zoneId);

    void deleteByTenantIdAndIdPassId(String tenantId, UUID passId);
}