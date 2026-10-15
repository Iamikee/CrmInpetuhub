package com.inpetu.connect.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), validationMessage(fieldError));
        }
        String message = errors.values().stream().findFirst()
                .orElse("Revise os dados informados e tente novamente.");
        logger.warn("Requisição rejeitada por validação nos campos: {}", errors.keySet());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", message, "errors", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        String cause = exception.getMostSpecificCause().getClass().getSimpleName();
        logger.warn("Requisição rejeitada por corpo inválido ({})", cause);
        return ResponseEntity.badRequest().body(Map.of(
                "message", "O formato dos dados enviados é inválido. Revise os campos e tente novamente."));
    }

    private String validationMessage(FieldError error) {
        String message = error.getDefaultMessage();
        if (message != null && (message.contains("celular") || message.contains("formato"))) {
            return message;
        }
        String code = error.getCode();
        return switch (code == null ? "" : code) {
            case "NotBlank", "NotNull", "NotEmpty" -> "Preencha este campo.";
            case "Email" -> "Informe um endereço de e-mail válido.";
            case "Size" -> "O valor informado está fora do tamanho permitido.";
            case "Pattern" -> "O formato informado é inválido.";
            default -> message == null ? "Valor inválido." : message;
        };
    }
}
