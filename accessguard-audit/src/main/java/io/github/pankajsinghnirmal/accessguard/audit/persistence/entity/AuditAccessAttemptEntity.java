package io.github.pankajsinghnirmal.accessguard.audit.persistence.entity;

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
        name = "audit_access_attempts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_audit_tenant_attempt", columnNames = {"tenant_id", "attempt_id"})
        },
        indexes = {
                @Index(name = "ix_audit_attempts_tenant_eval", columnList = "tenant_id,evaluated_at"),
                @Index(name = "ix_audit_attempts_tenant_decision", columnList = "tenant_id,decision"),
                @Index(name = "ix_audit_attempts_tenant_door", columnList = "tenant_id,door_code"),
                @Index(name = "ix_audit_attempts_tenant_device", columnList = "tenant_id,device_code"),
                @Index(name = "ix_audit_attempts_tenant_pass", columnList = "tenant_id,pass_ref")
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditAccessAttemptEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "event_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_audit_attempt_event")
    )
    private AuditEventEntity event;

    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    @Column(name = "door_code", nullable = false, length = 64)
    private String doorCode;

    @Column(name = "zone_code", length = 64)
    private String zoneCode;

    @Column(name = "pass_ref", length = 128)
    private String passRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 16)
    private AccessDecision decision;

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