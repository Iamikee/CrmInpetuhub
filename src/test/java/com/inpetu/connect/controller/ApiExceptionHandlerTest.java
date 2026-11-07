package com.inpetu.connect.controller;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void includesPortugueseReasonInBadRequestResponseStatusException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/reservas/3");
        String message = "Não é possível editar a reserva para uma data ou horário que já passou.";

        var response = handler.handleResponseStatusException(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, message), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals(message, response.getBody().message());
        assertEquals("/api/v1/reservas/3", response.getBody().path());
    }

    @Test
    void returnsHelpfulFieldValidationMessageWithBadRequestStatus() throws NoSuchMethodException {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "cadastro");
        bindingResult.addError(new FieldError("cadastro", "telefone", null,
                false, new String[] { "Pattern.cadastro.telefone", "Pattern.telefone", "Pattern.java.lang.String", "Pattern" },
                null, "Informe um celular no formato (48) 99999-9999."));

        Method method = ApiExceptionHandlerTest.class
                .getDeclaredMethod("returnsHelpfulFieldValidationMessageWithBadRequestStatus");
        var response = handler.handleValidation(
                new MethodArgumentNotValidException(new MethodParameter(Objects.requireNonNull(method), -1), bindingResult));
        var body = Objects.requireNonNull(response.getBody());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Informe um celular no formato (48) 99999-9999.", body.get("message"));
        assertEquals("Informe um celular no formato (48) 99999-9999.",
                ((Map<?, ?>) body.get("errors")).get("telefone"));
    }
}
