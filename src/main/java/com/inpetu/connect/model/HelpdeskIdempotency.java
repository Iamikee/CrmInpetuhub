package com.inpetu.connect.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "helpdesk_idempotency",
    uniqueConstraints = @UniqueConstraint(name = "uk_helpdesk_idempotency_scope_key",
        columnNames = {"scope_hash", "idempotency_key"}),
    indexes = {
        @Index(name = "idx_helpdesk_idempotency_result", columnList = "result_type,result_id"),
        @Index(name = "idx_helpdesk_idempotency_created", columnList = "created_at")
    })
@Getter
@Setter
@NoArgsConstructor
public class HelpdeskIdempotency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scope_hash", nullable = false, columnDefinition = "BINARY(32)")
    private byte[] scopeHash;

    @Column(name = "idempotency_key", nullable = false, columnDefinition = "BINARY(16)")
    private byte[] idempotencyKey;

    @Column(name = "request_hash", nullable = false, columnDefinition = "BINARY(32)")
    private byte[] requestHash;

    @Column(name = "result_type", nullable = false, length = 16)
    private String resultType;

    @Column(name = "result_id", nullable = false)
    private Long resultId;

    @Column(name = "response_status", nullable = false, columnDefinition = "SMALLINT")
    private short responseStatus;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime createdAt;
}
