package com.inpetu.connect.service;

import org.springframework.http.HttpStatus;

public class HelpdeskException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Long retryAfterSeconds;

    public HelpdeskException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    public HelpdeskException(HttpStatus status, String code, String message, Long retryAfterSeconds) {
        super(message);
        this.status = status;
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public static HelpdeskException notFound() {
        return new HelpdeskException(HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND",
            "Ticket não encontrado ou chave inválida.");
    }
}
