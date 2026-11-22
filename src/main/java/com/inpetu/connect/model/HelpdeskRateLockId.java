package com.inpetu.connect.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class HelpdeskRateLockId implements Serializable {
    @Column(name = "dimension", nullable = false, length = 5, columnDefinition = "VARCHAR(5)")
    private String dimension;

    @Column(name = "key_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String keyHash;

    public HelpdeskRateLockId(String dimension, String keyHash) {
        this.dimension = dimension;
        this.keyHash = keyHash;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof HelpdeskRateLockId that)) return false;
        return Objects.equals(dimension, that.dimension) && Objects.equals(keyHash, that.keyHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, keyHash);
    }
}
