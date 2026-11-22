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
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(validator.validate(new PerfilUsuarioRequest("Joana Silva", "+5548999991234", "InPETU", "Pesquisadora")).isEmpty());
        assertTrue(validator.validate(new PerfilUsuarioRequest("Joana Silva", "", "InPETU", "Pesquisadora")).isEmpty());
    }

    @Test
    void rejectsPhoneWithoutInternationalPrefixOrWithInvalidLength() {
        assertFalse(validator.validate(new PerfilUsuarioRequest("Joana Silva", "48999991234", "InPETU", "Pesquisadora")).isEmpty());
        assertFalse(validator.validate(new PerfilUsuarioRequest("Joana Silva", "+554899999123456789", "InPETU", "Pesquisadora")).isEmpty());
    }

    @Test
    void rejectsUnsupportedCountryCodesAcrossPhoneFlows() {
        String invalidPhone = "+999123456789";
        assertFalse(validator.validate(new CadastroPublicoRequest(
                "Joana Silva", "joana@example.test", invalidPhone, "InPETU", "Pesquisadora", "SenhaSegura123")).isEmpty());
        assertFalse(validator.validate(new PerfilUsuarioRequest(
                "Joana Silva", invalidPhone, "InPETU", "Pesquisadora")).isEmpty());
        assertFalse(validator.validate(new CadastroEdicaoRequest(
                "Joana Silva", "joana@example.test", invalidPhone, "InPETU", "Pesquisadora")).isEmpty());
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
                    "Joana Silva", "joana@example.test", invalidPhone, "InPETU", "Pesquisadora", "SenhaSegura123"), "telefone");
            assertPhoneRejected(new CadastroEdicaoRequest(
                    "Joana Silva", "joana@example.test", invalidPhone, "InPETU", "Pesquisadora"), "telefone");
            assertPhoneRejected(new PerfilUsuarioRequest(
                    "Joana Silva", invalidPhone, "InPETU", "Pesquisadora"), "telefone");
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
                        "Joana Silva", "joana@example.test", phone, "InPETU", "Pesquisadora", "SenhaSegura123"), "telefone"),
                new PhoneRequest(new CadastroEdicaoRequest(
                        "Joana Silva", "joana@example.test", phone, "InPETU", "Pesquisadora"), "telefone"),
                new PhoneRequest(new PerfilUsuarioRequest(
                        "Joana Silva", phone, "InPETU", "Pesquisadora"), "telefone"),
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

    @Test
    void enforcesFullNameEmailOrganizationAndRoleRulesAcrossWriteRequests() {
        assertFieldRejected(new CadastroPublicoRequest(
                "765765", "joana@example.org", "", "InPETU", "Pesquisadora", "SenhaSegura123"), "nomeCompleto");
        assertFieldRejected(new CadastroEdicaoRequest(
                "João 123 Silva", "joana@example.org", "", "InPETU", "Pesquisadora"), "nomeCompleto");
        assertFieldRejected(new PerfilUsuarioRequest(
                "Maria", "", "InPETU", "Pesquisadora"), "nomeCompleto");

        assertFieldRejected(new CadastroPublicoRequest(
                "Joana Silva", "765765", "", "InPETU", "Pesquisadora", "SenhaSegura123"), "email");
        assertFieldRejected(new CadastroEdicaoRequest(
                "Joana Silva", "a@b", "", "InPETU", "Pesquisadora"), "email");
        assertFieldRejected(new DuvidaRequest("a@b", "", "Mensagem de teste."), "emailContato");

        assertFieldRejected(new CadastroPublicoRequest(
                "Joana Silva", "joana@example.org", "", "765765", "Pesquisadora", "SenhaSegura123"), "organizacao");
        assertFieldRejected(new CadastroEdicaoRequest(
                "Joana Silva", "joana@example.org", "", "!!", "Pesquisadora"), "organizacao");
        assertFieldRejected(new PerfilUsuarioRequest(
                "Joana Silva", "", "InPETU", "1"), "cargoVinculo");
    }

    @Test
    void acceptsNormalizedNamesInternationalLettersAndTheAllowedThreeMOrganization() {
        var request = new CadastroPublicoRequest(
                "  João   da Silva  ", "Joana@Example.org", "", "3M", "P&D", "SenhaSegura123");

        assertTrue(validator.validate(request).isEmpty());
        assertEquals("João da Silva", request.nomeCompleto());
        assertEquals("joana@example.org", request.email());
    }

    @Test
    void enforcesExistingEmailAndOrganizationColumnLengths() {
        String emailAtLimit = "a".repeat(64) + "@" + "b".repeat(32) + ".cc";
        assertEquals(100, emailAtLimit.length());
        assertTrue(validator.validate(new CadastroEdicaoRequest(
                "Joana Silva", emailAtLimit, "", "InPETU", "Pesquisadora")).stream()
                .noneMatch(violation -> violation.getPropertyPath().toString().equals("email")));
        assertFieldRejected(new CadastroEdicaoRequest(
                "Joana Silva", "a".repeat(64) + "@" + "b".repeat(33) + ".cc", "", "InPETU", "Pesquisadora"), "email");
        assertFieldRejected(new CadastroPublicoRequest(
                "Joana Silva", "joana@example.org", "", "O".repeat(101), "Pesquisadora", "SenhaSegura123"), "organizacao");

        assertFieldRejected(new PerfilUsuarioRequest(
                "Joana Silva", "", "InPETU", "P".repeat(81)), "cargoVinculo");
    }

    private static void assertFieldRejected(Object request, String field) {
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals(field)),
                () -> request.getClass().getSimpleName() + " did not reject invalid " + field);
    }
}
