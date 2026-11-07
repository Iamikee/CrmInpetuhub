package com.inpetu.connect.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelefoneUtilsTest {
    @Test
    void convertsLegacyBrazilianPhonesToE164() {
        assertEquals("+5548999991234", TelefoneUtils.normalizarE164("(48) 99999-1234"));
        assertEquals("+5548999991234", TelefoneUtils.normalizarE164("48999991234"));
        assertEquals("+5548999991234", TelefoneUtils.normalizarE164("5548999991234"));
    }

    @Test
    void preservesInternationalCountryCodeAndHandlesOptionalValues() {
        assertEquals("+12125550100", TelefoneUtils.normalizarE164("+1 (212) 555-0100"));
        assertNull(TelefoneUtils.normalizarE164(null));
        assertNull(TelefoneUtils.normalizarE164("  "));
        assertTrue(TelefoneUtils.e164Valido("+12125550100"));
        assertTrue(TelefoneUtils.e164Valido("+351212345678"));
        assertFalse(TelefoneUtils.e164Valido("+999123456789"));
        assertFalse(TelefoneUtils.e164Valido("+5548123"));
    }
}
