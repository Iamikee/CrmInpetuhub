package com.inpetu.connect.controller;

import com.inpetu.connect.dto.*;
import com.inpetu.connect.service.HelpdeskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/helpdesk")
public class HelpdeskController {
    private final HelpdeskService helpdesk;

    public HelpdeskController(HelpdeskService helpdesk) {
        this.helpdesk = helpdesk;
    }

    @PostMapping("/tickets")
    public ResponseEntity<HelpdeskCreatedTicket> createTicket(
        @Valid @RequestBody HelpdeskCreateRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        HttpServletRequest servletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(helpdesk.createTicket(request, idempotencyKey, servletRequest));
    }

    @GetMapping("/tickets/{code}")
    public HelpdeskPublicTicket getPublicTicket(
        @PathVariable String code,
        @RequestHeader(value = "X-Ticket-Token", required = false) String token) {
        return helpdesk.getPublicTicket(code, token);
    }

    @GetMapping("/tickets/{code}/messages")
    public List<HelpdeskPublicMessage> getPublicMessages(
        @PathVariable String code,
        @RequestHeader(value = "X-Ticket-Token", required = false) String token,
        @RequestParam(defaultValue = "0") long afterId) {
        return helpdesk.getPublicMessages(code, token, afterId);
    }

    @PostMapping("/tickets/{code}/messages")
    public HelpdeskPublicMessage addPublicMessage(
        @PathVariable String code,
        @RequestHeader(value = "X-Ticket-Token", required = false) String token,
        @Valid @RequestBody HelpdeskMessageRequest request) {
        return helpdesk.addClientMessage(code, token, request.body());
    }

    @GetMapping("/staff/queue")
    @PreAuthorize("hasAnyRole('COORDENADOR','STAFF')")
    public List<HelpdeskStaffTicket> getStaffQueue(
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "50") int limit) {
        return helpdesk.getStaffQueue(status, limit);
    }

    @GetMapping("/staff/tickets/{id}")
    @PreAuthorize("hasAnyRole('COORDENADOR','STAFF')")
    public HelpdeskStaffTicket getStaffTicket(@PathVariable Long id) {
        return helpdesk.getStaffTicket(id);
    }

    @PostMapping("/staff/tickets/{id}/claim")
    @PreAuthorize("hasAnyRole('COORDENADOR','STAFF')")
    public HelpdeskStaffTicket claim(@PathVariable Long id, Authentication authentication) {
        return helpdesk.claim(id, authentication.getName());
    }

    @PostMapping("/staff/tickets/{id}/messages")
    @PreAuthorize("hasAnyRole('COORDENADOR','STAFF')")
    public HelpdeskStaffTicket addStaffMessage(@PathVariable Long id,
                                               @Valid @RequestBody HelpdeskMessageRequest request,
                                               Authentication authentication) {
        return helpdesk.addStaffMessage(id, authentication.getName(), request.body());
    }

    @PostMapping("/staff/tickets/{id}/resolve")
    @PreAuthorize("hasAnyRole('COORDENADOR','STAFF')")
    public HelpdeskStaffTicket resolve(@PathVariable Long id, Authentication authentication) {
        return helpdesk.resolve(id, authentication.getName());
    }

    @PostMapping("/staff/tickets/{id}/reopen")
    @PreAuthorize("hasAnyRole('COORDENADOR','STAFF')")
    public HelpdeskStaffTicket reopen(@PathVariable Long id, Authentication authentication) {
        return helpdesk.reopen(id, authentication.getName());
    }

    @GetMapping("/staff/tickets/{id}/history")
    @PreAuthorize("hasAnyRole('COORDENADOR','STAFF')")
    public List<HelpdeskStatusChange> getHistory(@PathVariable Long id) {
        return helpdesk.getStaffHistory(id);
    }
}
