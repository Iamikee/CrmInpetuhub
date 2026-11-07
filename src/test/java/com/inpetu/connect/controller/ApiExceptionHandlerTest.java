package com.inpetu.connect.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {
    @Test
    void includesPortugueseReasonInBadRequestResponseStatusException() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
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
}
