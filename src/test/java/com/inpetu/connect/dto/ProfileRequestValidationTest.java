package com.inpetu.connect.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

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
    void enforcesPasswordLengthAndConfirmationPresence() {
        assertFalse(validator.validate(new AlterarSenhaRequest("old", "short", "short")).isEmpty());
        assertFalse(validator.validate(new AlterarSenhaRequest("old", "long-enough", "")).isEmpty());
        assertTrue(validator.validate(new AlterarSenhaRequest("old", "long-enough", "long-enough")).isEmpty());
    }
}
