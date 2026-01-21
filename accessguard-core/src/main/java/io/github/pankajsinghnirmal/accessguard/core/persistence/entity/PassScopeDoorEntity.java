package io.github.pankajsinghnirmal.accessguard.core.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(
        name = "pass_scope_doors",
        indexes = {
                @Index(name = "ix_psd_tenant_pass", columnList = "tenant_id,pass_id"),
                @Index(name = "ix_psd_tenant_door", columnList = "tenant_id,door_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PassScopeDoorEntity {

    @EmbeddedId
    private PassScopeDoorId id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @MapsId("passId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "pass_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_psd_pass")
    )
    private PassEntity pass;

    @MapsId("doorId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "door_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_psd_door")
    )
    private DoorEntity door;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 128)
    private String createdBy;

    public static PassScopeDoorEntity of(String tenantId, PassEntity pass, DoorEntity door) {
        PassScopeDoorEntity entity = new PassScopeDoorEntity();
        entity.setTenantId(tenantId);
        entity.setPass(pass);
        entity.setDoor(door);
        entity.setId(new PassScopeDoorId(pass.getId(), door.getId()));
        return entity;
    }
}