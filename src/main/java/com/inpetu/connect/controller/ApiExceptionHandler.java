package com.inpetu.connect.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException exception, HttpServletRequest request) {
        HttpStatusCode status = exception.getStatusCode();
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        String error = httpStatus == null ? status.toString() : httpStatus.getReasonPhrase();
        String message = exception.getReason();
        if (message == null || message.isBlank()) {
            message = error;
        }

        return ResponseEntity.status(status).body(new ApiErrorResponse(
                status.value(), error, message, request.getRequestURI()));
    }

    public record ApiErrorResponse(int status, String error, String message, String path) {
    }
}
