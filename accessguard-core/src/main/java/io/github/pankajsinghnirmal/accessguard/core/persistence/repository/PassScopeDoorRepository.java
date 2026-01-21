package io.github.pankajsinghnirmal.accessguard.core.persistence.repository;

import io.github.pankajsinghnirmal.accessguard.core.persistence.entity.PassScopeDoorEntity;
import io.github.pankajsinghnirmal.accessguard.core.persistence.entity.PassScopeDoorId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PassScopeDoorRepository extends JpaRepository<PassScopeDoorEntity, PassScopeDoorId> {

    List<PassScopeDoorEntity> findByTenantIdAndIdPassId(String tenantId, UUID passId);

    boolean existsByTenantIdAndIdPassIdAndIdDoorId(String tenantId, UUID passId, UUID doorId);

    void deleteByTenantIdAndIdPassId(String tenantId, UUID passId);
}