package com.inpetu.connect.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "helpdesk_message",
    uniqueConstraints = @UniqueConstraint(name = "uk_helpdesk_message_ticket_seq", columnNames = {"ticket_id", "seq"}),
    indexes = @Index(name = "idx_helpdesk_message_ticket_seq", columnList = "ticket_id,seq"))
@Getter
@Setter
@NoArgsConstructor
public class HelpdeskMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private HelpdeskTicket ticket;

    @Column(name = "author_type", nullable = false, length = 8)
    private String authorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private Usuario authorUser;

    @Column(name = "author_name_snapshot", length = 100)
    private String authorNameSnapshot;

    @Column(name = "author_role_snapshot", length = 32)
    private String authorRoleSnapshot;

    @Column(nullable = false)
    private Long seq;

    @Column(nullable = false, length = 5000)
    private String body;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(3)")
    private LocalDateTime createdAt;
}
