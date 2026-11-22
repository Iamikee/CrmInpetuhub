package com.inpetu.connect.repository;

import com.inpetu.connect.model.HelpdeskTicket;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HelpdeskTicketRepository extends JpaRepository<HelpdeskTicket, Long> {
    Optional<HelpdeskTicket> findByPublicCode(String publicCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ticket from HelpdeskTicket ticket where ticket.id = :id")
    Optional<HelpdeskTicket> findLockedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ticket from HelpdeskTicket ticket where ticket.publicCode = :code")
    Optional<HelpdeskTicket> findLockedByPublicCode(@Param("code") String code);

    long countByIpHmacAndCreatedAtAfter(String ipHmac, LocalDateTime after);
    long countByEmailHmacAndCreatedAtAfter(String emailHmac, LocalDateTime after);

    Optional<HelpdeskTicket> findFirstByIpHmacAndCreatedAtAfterOrderByCreatedAtAsc(
        String ipHmac, LocalDateTime after);
    Optional<HelpdeskTicket> findFirstByEmailHmacAndCreatedAtAfterOrderByCreatedAtAsc(
        String emailHmac, LocalDateTime after);

    @Query("select ticket from HelpdeskTicket ticket order by ticket.updatedAt desc, ticket.id desc")
    List<HelpdeskTicket> findQueue(Pageable pageable);

    @Query("select ticket from HelpdeskTicket ticket where ticket.status = :status order by ticket.updatedAt desc, ticket.id desc")
    List<HelpdeskTicket> findQueueByStatus(@Param("status") String status, Pageable pageable);
}
