package com.inpetu.connect.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileRequestValidationTest {
    private static Validator validator;
    private static AutoCloseable validatorFactory;

    @BeforeAll
    static void createValidator() {
        var factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        validatorFactory = factory::close;
    }

    @AfterAll
    static void closeValidatorFactory() throws Exception {
        validatorFactory.close();
    }

    @Test
    void acceptsInternationalPhoneAndEmptyOptionalPhone() {
        assertTrue(validator.validate(new PerfilUsuarioRequest("Joana", "+5548999991234", "InPETU", "Pesquisadora")).isEmpty());
        assertTrue(validator.validate(new PerfilUsuarioRequest("Joana", "", "InPETU", "Pesquisadora")).isEmpty());
    }

    @Test
    void rejectsPhoneWithoutInternationalPrefixOrWithInvalidLength() {
        assertFalse(validator.validate(new PerfilUsuarioRequest("Joana", "48999991234", "InPETU", "Pesquisadora")).isEmpty());
        assertFalse(validator.validate(new PerfilUsuarioRequest("Joana", "+554899999123456789", "InPETU", "Pesquisadora")).isEmpty());
    }

    @Test
    void rejectsUnsupportedCountryCodesAcrossPhoneFlows() {
        String invalidPhone = "+999123456789";
        assertFalse(validator.validate(new CadastroPublicoRequest(
                "Joana", "joana@example.test", invalidPhone, "InPETU", "Pesquisadora", "SenhaSegura123")).isEmpty());
        assertFalse(validator.validate(new PerfilUsuarioRequest(
                "Joana", invalidPhone, "InPETU", "Pesquisadora")).isEmpty());
        assertFalse(validator.validate(new CadastroEdicaoRequest(
                "Joana", "joana@example.test", invalidPhone, "InPETU", "Pesquisadora")).isEmpty());
        assertFalse(validator.validate(new DuvidaRequest(
                "joana@example.test", invalidPhone, "Mensagem de teste.")).isEmpty());
        assertFalse(validator.validate(new ReservaRequest(
                "Reunião", "Teste", LocalDate.now().plusDays(1), LocalTime.of(10, 0),
                LocalTime.of(11, 0), 1, null, invalidPhone)).isEmpty());
        assertFalse(validator.validate(new ReservaEdicaoRequest(
                "Reunião", "Teste", LocalDate.now().plusDays(1), LocalTime.of(10, 0),
                LocalTime.of(11, 0), 1, null, invalidPhone)).isEmpty());
    }

    @Test
    void appliesBrazilianMobileValidationToEveryPhoneRequest() {
        List<String> invalidPhones = List.of(
                "+5500984260543",
                "+5520984260543",
                "+554833335555",
                "48984260543",
                "telefone");
        for (String invalidPhone : invalidPhones) {
            assertPhoneRejected(new CadastroPublicoRequest(
                    "Joana", "joana@example.test", invalidPhone, "InPETU", "Pesquisadora", "SenhaSegura123"), "telefone");
            assertPhoneRejected(new CadastroEdicaoRequest(
                    "Joana", "joana@example.test", invalidPhone, "InPETU", "Pesquisadora"), "telefone");
            assertPhoneRejected(new PerfilUsuarioRequest(
                    "Joana", invalidPhone, "InPETU", "Pesquisadora"), "telefone");
            assertPhoneRejected(new DuvidaRequest(
                    "joana@example.test", invalidPhone, "Mensagem de teste."), "telefoneContato");
            assertPhoneRejected(new ReservaRequest(
                    "Reunião", "Teste", LocalDate.now().plusDays(1), LocalTime.of(10, 0),
                    LocalTime.of(11, 0), 1, null, invalidPhone), "telefoneContato");
            assertPhoneRejected(new ReservaEdicaoRequest(
                    "Reunião", "Teste", LocalDate.now().plusDays(1), LocalTime.of(10, 0),
                    LocalTime.of(11, 0), 1, null, invalidPhone), "telefoneContato");
        }
    }

    @Test
    void acceptsValidE164AndEmptyOptionalPhoneAcrossAllRequestDtos() {
        for (String phone : List.of("+5548984260543", "")) {
            for (var entry : phoneRequests(phone)) {
                assertFalse(validator.validate(entry.request()).stream()
                        .anyMatch(violation -> violation.getPropertyPath().toString().equals(entry.field())),
                        () -> entry.request().getClass().getSimpleName() + " rejected " + entry.field());
            }
        }
    }

    private static List<PhoneRequest> phoneRequests(String phone) {
        return List.of(
                new PhoneRequest(new CadastroPublicoRequest(
                        "Joana", "joana@example.test", phone, "InPETU", "Pesquisadora", "SenhaSegura123"), "telefone"),
                new PhoneRequest(new CadastroEdicaoRequest(
                        "Joana", "joana@example.test", phone, "InPETU", "Pesquisadora"), "telefone"),
                new PhoneRequest(new PerfilUsuarioRequest(
                        "Joana", phone, "InPETU", "Pesquisadora"), "telefone"),
                new PhoneRequest(new DuvidaRequest(
                        "joana@example.test", phone, "Mensagem de teste."), "telefoneContato"),
                new PhoneRequest(new ReservaRequest(
                        "Reunião", "Teste", LocalDate.now().plusDays(1), LocalTime.of(10, 0),
                        LocalTime.of(11, 0), 1, null, phone), "telefoneContato"),
                new PhoneRequest(new ReservaEdicaoRequest(
                        "Reunião", "Teste", LocalDate.now().plusDays(1), LocalTime.of(10, 0),
                        LocalTime.of(11, 0), 1, null, phone), "telefoneContato"));
    }

    private static void assertPhoneRejected(Object request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals(field)),
                () -> request.getClass().getSimpleName() + " did not reject invalid phone in " + field);
    }

    private record PhoneRequest(Object request, String field) {
    }

    @Test
    void enforcesPasswordLengthAndConfirmationPresence() {
        assertFalse(validator.validate(new AlterarSenhaRequest("old", "short", "short")).isEmpty());
        assertFalse(validator.validate(new AlterarSenhaRequest("old", "long-enough", "")).isEmpty());
        assertTrue(validator.validate(new AlterarSenhaRequest("old", "long-enough", "long-enough")).isEmpty());
    }
}
