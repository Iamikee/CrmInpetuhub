package com.inpetu.connect.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "helpdesk_rate_lock")
@Getter
@Setter
@NoArgsConstructor
public class HelpdeskRateLock {
    @EmbeddedId
    private HelpdeskRateLockId id;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime updatedAt;
}
