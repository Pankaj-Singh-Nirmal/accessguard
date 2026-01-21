package io.github.pankajsinghnirmal.accessguard.core.persistence.repository;

import io.github.pankajsinghnirmal.accessguard.core.persistence.entity.DeviceEntity;
import io.github.pankajsinghnirmal.accessguard.core.persistence.entity.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<DeviceEntity, UUID> {

    Optional<DeviceEntity> findByTenantIdAndId(String tenantId, UUID id);

    Optional<DeviceEntity> findByTenantIdAndDeviceCode(String tenantId, String deviceCode);

    List<DeviceEntity> findByTenantIdAndStatus(String tenantId, DeviceStatus status);
}