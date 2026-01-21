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
        name = "pass_scope_zones",
        indexes = {
                @Index(name = "ix_psz_tenant_pass", columnList = "tenant_id,pass_id"),
                @Index(name = "ix_psz_tenant_zone", columnList = "tenant_id,zone_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PassScopeZoneEntity {

    @EmbeddedId
    private PassScopeZoneId id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @MapsId("passId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "pass_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_psz_pass")
    )
    private PassEntity pass;

    @MapsId("zoneId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "zone_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_psz_zone")
    )
    private ZoneEntity zone;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 128)
    private String createdBy;

    public static PassScopeZoneEntity of(String tenantId, PassEntity pass, ZoneEntity zone) {
        PassScopeZoneEntity e = new PassScopeZoneEntity();
        e.setTenantId(tenantId);
        e.setPass(pass);
        e.setZone(zone);
        e.setId(new PassScopeZoneId(pass.getId(), zone.getId()));
        return e;
    }
}