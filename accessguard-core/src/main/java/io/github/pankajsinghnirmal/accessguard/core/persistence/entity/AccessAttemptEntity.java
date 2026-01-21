package io.github.pankajsinghnirmal.accessguard.core.persistence.entity;

import io.github.pankajsinghnirmal.accessguard.shared.contracts.AccessDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "access_attempts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_attempt_tenant_attempt_id", columnNames = {"tenant_id", "attempt_id"})
        },
        indexes = {
                @Index(name = "ix_attempts_tenant_eval", columnList = "tenant_id,evaluated_at"),
                @Index(name = "ix_attempts_tenant_decision", columnList = "tenant_id,decision"),
                @Index(name = "ix_attempts_tenant_door", columnList = "tenant_id,door_id"),
                @Index(name = "ix_attempts_tenant_pass", columnList = "tenant_id,pass_id")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccessAttemptEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "device_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_attempt_device")
    )
    private DeviceEntity device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "door_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_attempt_door")
    )
    private DoorEntity door;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "pass_id",
            foreignKey = @ForeignKey(name = "fk_attempt_pass")
    )
    private PassEntity pass;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 16)
    private AccessDecision decision;

    /**
     * Keep as String (not enum) to allow easy evolution of reason codes without DB migrations.
     */
    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Column(name = "correlation_id", length = 128)
    private String correlationId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}