package com.inpetu.connect.service;

import com.inpetu.connect.dto.*;
import com.inpetu.connect.model.*;
import com.inpetu.connect.repository.*;
import com.inpetu.connect.security.ClientIpResolver;
import com.inpetu.connect.util.TelefoneUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.*;

@Service
public class HelpdeskService {
    private static final int MESSAGE_PAGE_SIZE = 100;
    private static final String STATUS_OPEN = "ABERTO";
    private static final String STATUS_IN_PROGRESS = "EM_ATENDIMENTO";
    private static final String STATUS_WAITING_CLIENT = "AGUARDANDO_CLIENTE";
    private static final String STATUS_RESOLVED = "RESOLVIDO";
    private static final String STATUS_REOPENED = "REABERTO";

    private final HelpdeskTicketRepository tickets;
    private final HelpdeskMessageRepository messages;
    private final HelpdeskStatusHistoryRepository statusHistory;
    private final HelpdeskIdempotencyRepository idempotency;
    private final HelpdeskRateLockRepository rateLocks;
    private final HelpdeskRateLockService rateLockService;
    private final LogAuditoriaRepository audit;
    private final UsuarioRepository users;
    private final HelpdeskCryptography cryptography;
    private final TransactionTemplate createTransaction;
    private final boolean trustProxyHeaders;
    private final Set<String> trustedProxies;
    private final int ipLimit;
    private final int emailLimit;

    public HelpdeskService(HelpdeskTicketRepository tickets,
                           HelpdeskMessageRepository messages,
                           HelpdeskStatusHistoryRepository statusHistory,
                           HelpdeskIdempotencyRepository idempotency,
                           HelpdeskRateLockRepository rateLocks,
                           HelpdeskRateLockService rateLockService,
                           LogAuditoriaRepository audit,
                           UsuarioRepository users,
                           HelpdeskCryptography cryptography,
                           PlatformTransactionManager transactionManager,
                           @Value("${app.trust-proxy-headers:false}") boolean trustProxyHeaders,
                           @Value("${app.security.trusted-proxies:}") String trustedProxyConfig,
                           @Value("${app.helpdesk.rate.ip-per-hour:20}") int ipLimit,
                           @Value("${app.helpdesk.rate.email-per-hour:5}") int emailLimit) {
        this.tickets = tickets;
        this.messages = messages;
        this.statusHistory = statusHistory;
        this.idempotency = idempotency;
        this.rateLocks = rateLocks;
        this.rateLockService = rateLockService;
        this.audit = audit;
        this.users = users;
        this.cryptography = cryptography;
        this.trustProxyHeaders = trustProxyHeaders;
        this.trustedProxies = ClientIpResolver.parseTrustedProxies(trustedProxyConfig);
        if (ipLimit < 1 || emailLimit < 1) {
            throw new IllegalArgumentException("Os limites do Help Desk devem ser positivos.");
        }
        this.ipLimit = ipLimit;
        this.emailLimit = emailLimit;
        this.createTransaction = new TransactionTemplate(transactionManager);
        this.createTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    public HelpdeskCreatedTicket createTicket(HelpdeskCreateRequest request, String idempotencyHeader,
                                               HttpServletRequest servletRequest) {
        UUID key = parseIdempotencyKey(idempotencyHeader);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String name = request.name().trim();
        String subject = request.subject().trim();
        String body = request.motivo().trim();
        String phone = normalizePhone(request.phone());
        String extension = request.extension() == null || request.extension().isBlank()
            ? null : request.extension().trim();
        String ip = ClientIpResolver.resolve(servletRequest, trustProxyHeaders, trustedProxies);
        if (ip == null || ip.isBlank()) {
            throw new HelpdeskException(HttpStatus.SERVICE_UNAVAILABLE, "CLIENT_IP_UNAVAILABLE",
                "Não foi possível validar a origem da solicitação.");
        }

        String ipHmac = cryptography.hmacHex("inpetu:helpdesk:rate-limit:ip:v1", ip.trim());
        String emailHmac = cryptography.hmacHex("inpetu:helpdesk:rate-limit:email:v1", email);
        byte[] scopeHash = cryptography.hmac("helpdesk:idempotency-scope:v1", "create-ticket");
        byte[] keyBytes = cryptography.uuidBytes(key);
        byte[] requestHash = cryptography.sha256(canonical(name, email, subject, body, phone, extension));
        Optional<HelpdeskIdempotency> existing = idempotency.findByScopeHashAndIdempotencyKey(scopeHash, keyBytes);
        if (existing.isPresent()) {
            throw replay(existing.get(), requestHash);
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return createTransaction.execute(status ->
                    createTicketInTransaction(name, email, subject, body, phone, extension, ipHmac,
                        emailHmac, scopeHash, keyBytes, requestHash));
            } catch (DataIntegrityViolationException conflict) {
                Optional<HelpdeskIdempotency> winner =
                    idempotency.findByScopeHashAndIdempotencyKey(scopeHash, keyBytes);
                if (winner.isPresent()) {
                    throw replay(winner.get(), requestHash);
                }
                throw conflict;
            } catch (RuntimeException failure) {
                if (isRetryableDeadlock(failure) && attempt < 2) {
                    pauseBeforeRetry(attempt);
                    continue;
                }
                if (failure instanceof DataAccessException) {
                    throw new HelpdeskException(HttpStatus.SERVICE_UNAVAILABLE, "RATE_LIMIT_UNAVAILABLE",
                        "O limite de solicitações está temporariamente indisponível.");
                }
                throw failure;
            }
        }
        throw new IllegalStateException("A transação de criação do ticket terminou sem resultado.");
    }

    private HelpdeskCreatedTicket createTicketInTransaction(String name, String email, String subject,
                                                              String body, String phone, String extension,
                                                              String ipHmac, String emailHmac,
                                                              byte[] scopeHash, byte[] keyBytes,
                                                              byte[] requestHash) {
        Optional<HelpdeskIdempotency> prior =
            idempotency.findByScopeHashAndIdempotencyKey(scopeHash, keyBytes);
        if (prior.isPresent()) {
            throw replay(prior.get(), requestHash);
        }

        LocalDateTime now;
        try {
            ensureRateLocks(ipHmac, emailHmac);
            acquireRateLocks(ipHmac, emailHmac);
            Optional<HelpdeskIdempotency> afterLock =
                idempotency.findByScopeHashAndIdempotencyKey(scopeHash, keyBytes);
            if (afterLock.isPresent()) {
                throw replay(afterLock.get(), requestHash);
            }
            now = nowUtc();
            LocalDateTime windowStart = now.minusMinutes(60);
            enforceIpLimit(ipHmac, windowStart, now);
            enforceEmailLimit(emailHmac, windowStart, now);
        } catch (org.springframework.dao.DataAccessException databaseFailure) {
            if (isRetryableDeadlock(databaseFailure)) {
                throw databaseFailure;
            }
            throw new HelpdeskException(HttpStatus.SERVICE_UNAVAILABLE, "RATE_LIMIT_UNAVAILABLE",
                "O limite de solicitações está temporariamente indisponível.");
        }

        byte[] tokenBytes = cryptography.newTokenBytes();
        String token = cryptography.encodeToken(tokenBytes);
        HelpdeskTicket ticket = new HelpdeskTicket();
        ticket.setPublicCode(cryptography.newPublicCode());
        ticket.setTokenHash(cryptography.tokenHashBytes(tokenBytes));
        ticket.setName(name);
        ticket.setContactEmail(email);
        ticket.setEmailVerified(false);
        ticket.setSubject(subject);
        ticket.setPhoneE164(phone);
        ticket.setExtension(extension);
        ticket.setStatus(STATUS_OPEN);
        ticket.setIpHmac(ipHmac);
        ticket.setEmailHmac(emailHmac);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        ticket.setVersion(0L);
        ticket = tickets.saveAndFlush(ticket);

        saveMessage(ticket, "CLIENT", null, "Solicitante", null, 1, body, now);
        saveHistory(ticket, null, STATUS_OPEN, "CLIENT", null, "TICKET_CREATED", now);
        saveAudit(ticket, null, "TICKET_CREATED", maskedContact(ticket));

        HelpdeskIdempotency record = new HelpdeskIdempotency();
        record.setScopeHash(scopeHash);
        record.setIdempotencyKey(keyBytes);
        record.setRequestHash(requestHash);
        record.setResultType("TICKET");
        record.setResultId(ticket.getId());
        record.setResponseStatus((short) HttpStatus.CREATED.value());
        record.setCreatedAt(now);
        idempotency.saveAndFlush(record);
        return new HelpdeskCreatedTicket(ticket.getPublicCode(), token, ticket.getStatus(), now);
    }

    @Transactional(readOnly = true)
    public HelpdeskPublicTicket getPublicTicket(String code, String token) {
        HelpdeskTicket ticket = authenticatePublic(code, token);
        return publicTicket(ticket, latestMessages(ticket.getId(), MESSAGE_PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public List<HelpdeskPublicMessage> getPublicMessages(String code, String token, long afterSeq) {
        HelpdeskTicket ticket = authenticatePublic(code, token);
        if (afterSeq < 0) {
            throw new HelpdeskException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "O cursor de mensagens é inválido.");
        }
        return messages.findPublicAfter(ticket.getId(), afterSeq, PageRequest.of(0, MESSAGE_PAGE_SIZE))
            .stream().map(this::publicMessage).toList();
    }

    @Transactional
    public HelpdeskPublicMessage addClientMessage(String code, String token, String body) {
        HelpdeskTicket ticket = tickets.findLockedByPublicCode(code).orElse(null);
        if (!cryptography.matchesToken(token, ticket == null ? null : ticket.getTokenHash())) {
            throw HelpdeskException.notFound();
        }
        LocalDateTime now = nowUtc();
        String previousStatus = ticket.getStatus();
        String nextStatus = previousStatus;
        if (STATUS_RESOLVED.equals(previousStatus)) {
            nextStatus = STATUS_REOPENED;
            ticket.setAssignedUser(null);
            ticket.setResolvedAt(null);
        } else if (STATUS_WAITING_CLIENT.equals(previousStatus)) {
            nextStatus = STATUS_IN_PROGRESS;
        } else if (!Set.of(STATUS_OPEN, STATUS_IN_PROGRESS, STATUS_REOPENED).contains(previousStatus)) {
            throw invalidTransition();
        }
        ticket.setUpdatedAt(now);
        if (!previousStatus.equals(nextStatus)) {
            ticket.setStatus(nextStatus);
            saveHistory(ticket, previousStatus, nextStatus, "CLIENT", null,
                STATUS_RESOLVED.equals(previousStatus) ? "CLIENT_REPLY_AFTER_RESOLUTION" : "CLIENT_REPLY", now);
            saveAudit(ticket, null, "TICKET_STATUS_CHANGED", "status=" + previousStatus + "->" + nextStatus);
        }
        long seq = messages.findLastSequence(ticket.getId()) + 1;
        HelpdeskMessage message = saveMessage(ticket, "CLIENT", null, "Solicitante", null, seq,
            body.trim(), now);
        tickets.save(ticket);
        return publicMessage(message);
    }

    @Transactional(readOnly = true)
    public List<HelpdeskStaffTicket> getStaffQueue(String status, int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, 100));
        List<HelpdeskTicket> queue = status == null || status.isBlank()
            ? tickets.findQueue(PageRequest.of(0, limit))
            : tickets.findQueueByStatus(status, PageRequest.of(0, limit));
        return queue.stream().map(ticket -> staffTicket(ticket, latestMessages(ticket.getId(), 1))).toList();
    }

    @Transactional(readOnly = true)
    public HelpdeskStaffTicket getStaffTicket(Long id) {
        HelpdeskTicket ticket = tickets.findById(id).orElseThrow(() ->
            new HelpdeskException(HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND", "Ticket não encontrado."));
        return staffTicket(ticket, latestMessages(ticket.getId(), MESSAGE_PAGE_SIZE));
    }

    @Transactional
    public HelpdeskStaffTicket claim(Long id, String staffEmail) {
        Usuario staff = requireStaff(staffEmail);
        HelpdeskTicket ticket = lockedStaffTicket(id);
        if (!Set.of(STATUS_OPEN, STATUS_REOPENED).contains(ticket.getStatus())) {
            throw new HelpdeskException(HttpStatus.CONFLICT, "TICKET_ALREADY_CLAIMED",
                "Este ticket não está disponível para assumir.");
        }
        String previous = ticket.getStatus();
        LocalDateTime now = nowUtc();
        ticket.setAssignedUser(staff);
        ticket.setStatus(STATUS_IN_PROGRESS);
        ticket.setUpdatedAt(now);
        tickets.save(ticket);
        saveHistory(ticket, previous, STATUS_IN_PROGRESS, "STAFF", staff, "STAFF_CLAIM", now);
        saveAudit(ticket, staff, "TICKET_STATUS_CHANGED", "status=" + previous + "->" + STATUS_IN_PROGRESS);
        return staffTicket(ticket, latestMessages(ticket.getId(), MESSAGE_PAGE_SIZE));
    }

    @Transactional
    public HelpdeskStaffTicket addStaffMessage(Long id, String staffEmail, String body) {
        Usuario staff = requireStaff(staffEmail);
        HelpdeskTicket ticket = lockedStaffTicket(id);
        if (STATUS_RESOLVED.equals(ticket.getStatus())) {
            throw new HelpdeskException(HttpStatus.CONFLICT, "TICKET_RESOLVED",
                "Reabra o ticket antes de responder.");
        }
        if (!Set.of(STATUS_IN_PROGRESS, STATUS_WAITING_CLIENT).contains(ticket.getStatus())) {
            throw new HelpdeskException(HttpStatus.CONFLICT, "TICKET_NOT_CLAIMED",
                "Assuma o ticket antes de responder.");
        }
        requireAssignee(ticket, staff);
        LocalDateTime now = nowUtc();
        String previous = ticket.getStatus();
        long seq = messages.findLastSequence(ticket.getId()) + 1;
        saveMessage(ticket, "STAFF", staff, staff.getNomeCompleto(), roleLabel(staff), seq,
            body.trim(), now);
        if (STATUS_IN_PROGRESS.equals(previous)) {
            ticket.setStatus(STATUS_WAITING_CLIENT);
            saveHistory(ticket, previous, STATUS_WAITING_CLIENT, "STAFF", staff, "STAFF_REPLY", now);
            saveAudit(ticket, staff, "TICKET_STATUS_CHANGED",
                "status=" + previous + "->" + STATUS_WAITING_CLIENT);
        }
        ticket.setUpdatedAt(now);
        tickets.save(ticket);
        return staffTicket(ticket, latestMessages(ticket.getId(), MESSAGE_PAGE_SIZE));
    }

    @Transactional
    public HelpdeskStaffTicket resolve(Long id, String staffEmail) {
        Usuario staff = requireStaff(staffEmail);
        HelpdeskTicket ticket = lockedStaffTicket(id);
        String previous = ticket.getStatus();
        if (STATUS_RESOLVED.equals(previous)) {
            throw new HelpdeskException(HttpStatus.CONFLICT, "TICKET_ALREADY_RESOLVED",
                "Este ticket já está resolvido.");
        }
        if (!Set.of(STATUS_IN_PROGRESS, STATUS_WAITING_CLIENT).contains(previous)) {
            throw new HelpdeskException(HttpStatus.CONFLICT, "TICKET_NOT_CLAIMED",
                "Assuma o ticket antes de resolvê-lo.");
        }
        requireAssignee(ticket, staff);
        LocalDateTime now = nowUtc();
        ticket.setStatus(STATUS_RESOLVED);
        ticket.setResolvedAt(now);
        ticket.setUpdatedAt(now);
        tickets.save(ticket);
        saveHistory(ticket, previous, STATUS_RESOLVED, "STAFF", staff, "STAFF_RESOLVED", now);
        saveAudit(ticket, staff, "TICKET_STATUS_CHANGED", "status=" + previous + "->" + STATUS_RESOLVED);
        return staffTicket(ticket, latestMessages(ticket.getId(), MESSAGE_PAGE_SIZE));
    }

    @Transactional
    public HelpdeskStaffTicket reopen(Long id, String staffEmail) {
        Usuario staff = requireStaff(staffEmail);
        HelpdeskTicket ticket = lockedStaffTicket(id);
        if (!STATUS_RESOLVED.equals(ticket.getStatus())) {
            throw invalidTransition();
        }
        LocalDateTime now = nowUtc();
        ticket.setAssignedUser(null);
        ticket.setResolvedAt(null);
        ticket.setStatus(STATUS_REOPENED);
        ticket.setUpdatedAt(now);
        tickets.save(ticket);
        saveHistory(ticket, STATUS_RESOLVED, STATUS_REOPENED, "STAFF", staff, "STAFF_REOPENED", now);
        saveAudit(ticket, staff, "TICKET_STATUS_CHANGED",
            "status=" + STATUS_RESOLVED + "->" + STATUS_REOPENED);
        return staffTicket(ticket, latestMessages(ticket.getId(), MESSAGE_PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public List<HelpdeskStatusChange> getStaffHistory(Long id) {
        if (!tickets.existsById(id)) {
            throw new HelpdeskException(HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND", "Ticket não encontrado.");
        }
        return statusHistory.findAllByTicket(id, PageRequest.of(0, 500)).stream()
            .map(history -> new HelpdeskStatusChange(history.getFromStatus(), history.getToStatus(),
                history.getActorType(),
                history.getActorUser() == null ? null : history.getActorUser().getNomeCompleto(),
                history.getReasonCode(), history.getCreatedAt()))
            .toList();
    }

    private HelpdeskTicket authenticatePublic(String code, String token) {
        HelpdeskTicket ticket = tickets.findByPublicCode(code).orElse(null);
        if (!cryptography.matchesToken(token, ticket == null ? null : ticket.getTokenHash())) {
            throw HelpdeskException.notFound();
        }
        return ticket;
    }

    private HelpdeskTicket lockedStaffTicket(Long id) {
        return tickets.findLockedById(id).orElseThrow(() ->
            new HelpdeskException(HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND", "Ticket não encontrado."));
    }

    private Usuario requireStaff(String email) {
        Usuario user = users.findByEmailIgnoreCase(email).orElseThrow(() ->
            new HelpdeskException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Acesso restrito à equipe."));
        if (!Set.of(1, 3).contains(user.getIdPerfil())) {
            throw new HelpdeskException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Acesso restrito à equipe.");
        }
        return user;
    }

    private void requireAssignee(HelpdeskTicket ticket, Usuario staff) {
        if (ticket.getAssignedUser() == null
            || !Objects.equals(ticket.getAssignedUser().getIdUsuario(), staff.getIdUsuario())) {
            throw new HelpdeskException(HttpStatus.FORBIDDEN, "NOT_TICKET_ASSIGNEE",
                "Assuma o ticket antes de executar esta ação.");
        }
    }

    private void ensureRateLocks(String ipHmac, String emailHmac) {
        List<HelpdeskRateLockId> ids = rateLockIds(ipHmac, emailHmac);
        for (HelpdeskRateLockId id : ids) {
            try {
                rateLockService.ensureLock(id.getDimension(), id.getKeyHash());
            } catch (DataIntegrityViolationException concurrentInsert) {
                if (!rateLocks.existsById(id)) {
                    throw concurrentInsert;
                }
            }
        }
    }

    private void acquireRateLocks(String ipHmac, String emailHmac) {
        for (HelpdeskRateLockId id : rateLockIds(ipHmac, emailHmac)) {
            HelpdeskRateLock row = rateLocks.findLocked(id.getDimension(), id.getKeyHash())
                .orElseThrow(() -> new IllegalStateException("A linha de lock do Help Desk não foi criada."));
            row.setUpdatedAt(nowUtc());
            rateLocks.save(row);
        }
    }

    private List<HelpdeskRateLockId> rateLockIds(String ipHmac, String emailHmac) {
        return List.of(
            new HelpdeskRateLockId("EMAIL", emailHmac),
            new HelpdeskRateLockId("IP", ipHmac)
        ).stream().sorted(Comparator.comparing(HelpdeskRateLockId::getDimension)
            .thenComparing(HelpdeskRateLockId::getKeyHash)).toList();
    }

    private void enforceIpLimit(String ipHmac, LocalDateTime since, LocalDateTime now) {
        if (tickets.countByIpHmacAndCreatedAtAfter(ipHmac, since) >= ipLimit) {
            long retry = tickets.findFirstByIpHmacAndCreatedAtAfterOrderByCreatedAtAsc(ipHmac, since)
                .map(ticket -> retryAfter(ticket.getCreatedAt(), now)).orElse(60L);
            throw tooMany("RATE_LIMIT_IP", retry);
        }
    }

    private void enforceEmailLimit(String emailHmac, LocalDateTime since, LocalDateTime now) {
        if (tickets.countByEmailHmacAndCreatedAtAfter(emailHmac, since) >= emailLimit) {
            long retry = tickets.findFirstByEmailHmacAndCreatedAtAfterOrderByCreatedAtAsc(emailHmac, since)
                .map(ticket -> retryAfter(ticket.getCreatedAt(), now)).orElse(60L);
            throw tooMany("RATE_LIMIT_EMAIL", retry);
        }
    }

    private long retryAfter(LocalDateTime oldest, LocalDateTime now) {
        long millis = Duration.between(now, oldest.plusMinutes(60)).toMillis();
        return Math.max(1, (millis + 999) / 1000);
    }

    private HelpdeskException tooMany(String code, long retrySeconds) {
        return new HelpdeskException(HttpStatus.TOO_MANY_REQUESTS,
            code.equals("RATE_LIMIT_IP") ? "RATE_LIMIT_IP_EXCEEDED" : "RATE_LIMIT_EMAIL_EXCEEDED",
            "Limite de solicitações atingido. Tente novamente mais tarde.", retrySeconds);
    }

    private HelpdeskMessage saveMessage(HelpdeskTicket ticket, String authorType, Usuario author,
                                        String authorName, String authorRole, long seq, String body,
                                        LocalDateTime now) {
        HelpdeskMessage message = new HelpdeskMessage();
        message.setTicket(ticket);
        message.setAuthorType(authorType);
        message.setAuthorUser(author);
        message.setAuthorNameSnapshot(authorName);
        message.setAuthorRoleSnapshot(authorRole);
        message.setSeq(seq);
        message.setBody(body);
        message.setCreatedAt(now);
        return messages.save(message);
    }

    private List<HelpdeskMessage> latestMessages(Long ticketId, int limit) {
        List<HelpdeskMessage> latest = messages.findLatestByTicket(ticketId, PageRequest.of(0, limit));
        Collections.reverse(latest);
        return latest;
    }

    private void saveHistory(HelpdeskTicket ticket, String from, String to, String actorType,
                             Usuario actor, String reason, LocalDateTime now) {
        HelpdeskStatusHistory history = new HelpdeskStatusHistory();
        history.setTicket(ticket);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setActorType(actorType);
        history.setActorUser(actor);
        history.setReasonCode(reason);
        history.setCorrelationId(UUID.randomUUID().toString());
        history.setCreatedAt(now);
        statusHistory.save(history);
    }

    private void saveAudit(HelpdeskTicket ticket, Usuario actor, String action, String details) {
        LogAuditoria record = new LogAuditoria();
        record.setTabelaAfetada("helpdesk_ticket");
        record.setIdRegistroAfetado(ticket.getId() <= Integer.MAX_VALUE ? ticket.getId().intValue() : null);
        record.setAcao(action);
        record.setDetalhesAlteracao("ticket=" + ticket.getPublicCode() + "; " + details);
        record.setDataHora(nowUtc());
        record.setIdUsuarioResponsavel(actor == null ? null : actor.getIdUsuario());
        audit.save(record);
    }

    private String maskedContact(HelpdeskTicket ticket) {
        return "email=" + maskEmail(ticket.getContactEmail()) + "; telefone=" + maskPhone(ticket.getPhoneE164());
    }

    private String maskEmail(String email) {
        int separator = email == null ? -1 : email.indexOf('@');
        if (separator <= 0) {
            return "***";
        }
        String domain = email.substring(separator + 1);
        int dot = domain.lastIndexOf('.');
        return email.charAt(0) + "***@***" + (dot >= 0 ? domain.substring(dot) : "");
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "***";
        }
        String digits = phone.replaceAll("\\D", "");
        return digits.length() < 3 ? "***" : "***" + digits.substring(digits.length() - 2);
    }

    private HelpdeskPublicTicket publicTicket(HelpdeskTicket ticket, List<HelpdeskMessage> ticketMessages) {
        return new HelpdeskPublicTicket(ticket.getPublicCode(), ticket.getSubject(), ticket.getStatus(),
            ticket.getCreatedAt(), ticket.getUpdatedAt(), ticketMessages.stream().map(this::publicMessage).toList());
    }

    private HelpdeskPublicMessage publicMessage(HelpdeskMessage message) {
        String authorName = switch (message.getAuthorType()) {
            case "CLIENT" -> "Você";
            case "STAFF" -> "Equipe InPETU";
            default -> "Equipe";
        };
        return new HelpdeskPublicMessage(message.getSeq(), message.getAuthorType(), authorName,
            message.getBody(), message.getCreatedAt());
    }

    private HelpdeskStaffTicket staffTicket(HelpdeskTicket ticket, List<HelpdeskMessage> ticketMessages) {
        Usuario assigned = ticket.getAssignedUser();
        return new HelpdeskStaffTicket(ticket.getId(), ticket.getPublicCode(), ticket.getName(),
            ticket.getContactEmail(), ticket.isEmailVerified(), ticket.getPhoneE164(), ticket.getExtension(),
            ticket.getSubject(), ticket.getStatus(), assigned == null ? null : assigned.getIdUsuario(),
            assigned == null ? null : assigned.getNomeCompleto(), ticket.getCreatedAt(), ticket.getUpdatedAt(),
            ticketMessages.stream().map(this::publicMessage).toList());
    }

    private String roleLabel(Usuario staff) {
        return staff.getIdPerfil() == 1 ? "COORDENADOR" : "STAFF";
    }

    private String normalizePhone(String raw) {
        String normalized = TelefoneUtils.normalizarE164(raw);
        if (normalized != null && !TelefoneUtils.e164Valido(normalized)) {
            throw new HelpdeskException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Informe um celular brasileiro válido.");
        }
        return normalized;
    }

    private UUID parseIdempotencyKey(String raw) {
        try {
            String value = raw == null ? "" : raw.trim();
            if (!value.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")) {
                throw new IllegalArgumentException();
            }
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new HelpdeskException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_INVALID",
                "Envie uma chave Idempotency-Key UUID v4 válida.");
        }
    }

    private HelpdeskException replay(HelpdeskIdempotency existing, byte[] requestHash) {
        if (!java.security.MessageDigest.isEqual(existing.getRequestHash(), requestHash)) {
            return new HelpdeskException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                "A chave de envio já foi usada com outros dados.");
        }
        return new HelpdeskException(HttpStatus.CONFLICT, "IDEMPOTENCY_REPLAY_TOKEN_UNAVAILABLE",
            "A solicitação já foi processada. O token não pode ser recuperado nesta resposta.");
    }

    private byte[] canonical(String name, String email, String subject, String body, String phone,
                             String extension) {
        List<byte[]> fields = List.of(name, email, subject, body, phone == null ? "" : phone,
            extension == null ? "" : extension).stream()
            .map(value -> value.getBytes(StandardCharsets.UTF_8)).toList();
        int size = fields.stream().mapToInt(field -> Integer.BYTES + field.length).sum();
        ByteBuffer buffer = ByteBuffer.allocate(size);
        fields.forEach(field -> buffer.putInt(field.length).put(field));
        return buffer.array();
    }

    private HelpdeskException invalidTransition() {
        return new HelpdeskException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
            "A ação não é permitida no estado atual do ticket.");
    }

    private boolean isRetryableDeadlock(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof DeadlockLoserDataAccessException
                || cause instanceof CannotAcquireLockException) {
                return true;
            }
            if (cause instanceof SQLException sqlException
                && ("40001".equals(sqlException.getSQLState())
                    || sqlException.getErrorCode() == 1205
                    || sqlException.getErrorCode() == 1213)) {
                return true;
            }
        }
        return false;
    }

    private void pauseBeforeRetry(int attempt) {
        try {
            Thread.sleep(25L * (attempt + 1));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new HelpdeskException(HttpStatus.SERVICE_UNAVAILABLE, "RATE_LIMIT_UNAVAILABLE",
                "O limite de solicitações está temporariamente indisponível.");
        }
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
    }
}
