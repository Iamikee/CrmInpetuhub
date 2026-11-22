package com.inpetu.connect.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "helpdesk_ticket", indexes = {
    @Index(name = "idx_helpdesk_ticket_status_updated", columnList = "status,updated_at,id"),
    @Index(name = "idx_helpdesk_ticket_assigned_status", columnList = "assigned_user_id,status,updated_at,id"),
    @Index(name = "idx_helpdesk_ticket_ip_created", columnList = "ip_hmac,created_at"),
    @Index(name = "idx_helpdesk_ticket_email_created", columnList = "email_hmac,created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class HelpdeskTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_code", nullable = false, unique = true, length = 29, columnDefinition = "CHAR(29)")
    private String publicCode;

    @Column(name = "token_hash", nullable = false, unique = true, columnDefinition = "BINARY(32)")
    private byte[] tokenHash;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "contact_email", nullable = false, length = 254)
    private String contactEmail;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(nullable = false, length = 160)
    private String subject;

    @Column(name = "phone_e164", length = 16)
    private String phoneE164;

    @Column(length = 10)
    private String extension;

    @Column(nullable = false, length = 24)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private Usuario assignedUser;

    @Column(name = "ip_hmac", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String ipHmac;

    @Column(name = "email_hmac", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String emailHmac;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at", columnDefinition = "DATETIME(3)")
    private LocalDateTime resolvedAt;

    @Version
    @Column(nullable = false)
    private Long version;
}
