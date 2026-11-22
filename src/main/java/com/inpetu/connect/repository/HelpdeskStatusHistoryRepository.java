package com.inpetu.connect.repository;

import com.inpetu.connect.model.HelpdeskStatusHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HelpdeskStatusHistoryRepository extends JpaRepository<HelpdeskStatusHistory, Long> {
    @Query("select history from HelpdeskStatusHistory history where history.ticket.id = :ticketId order by history.createdAt asc, history.id asc")
    List<HelpdeskStatusHistory> findAllByTicket(@Param("ticketId") Long ticketId, Pageable pageable);
}
