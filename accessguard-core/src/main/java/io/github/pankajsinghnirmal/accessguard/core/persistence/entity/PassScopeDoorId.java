package io.github.pankajsinghnirmal.accessguard.core.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class PassScopeDoorId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "pass_id", nullable = false)
    private UUID passId;

    @Column(name = "door_id", nullable = false)
    private UUID doorId;
}