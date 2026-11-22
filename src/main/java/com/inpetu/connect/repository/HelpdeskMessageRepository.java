package com.inpetu.connect.repository;

import com.inpetu.connect.model.HelpdeskMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HelpdeskMessageRepository extends JpaRepository<HelpdeskMessage, Long> {
    @Query("select coalesce(max(message.seq), 0) from HelpdeskMessage message where message.ticket.id = :ticketId")
    long findLastSequence(@Param("ticketId") Long ticketId);

    @Query("select message from HelpdeskMessage message where message.ticket.id = :ticketId and message.seq > :afterSeq order by message.seq asc")
    List<HelpdeskMessage> findPublicAfter(@Param("ticketId") Long ticketId,
                                          @Param("afterSeq") long afterSeq,
                                          Pageable pageable);

    @Query("select message from HelpdeskMessage message where message.ticket.id = :ticketId order by message.seq desc")
    List<HelpdeskMessage> findLatestByTicket(@Param("ticketId") Long ticketId, Pageable pageable);
}
