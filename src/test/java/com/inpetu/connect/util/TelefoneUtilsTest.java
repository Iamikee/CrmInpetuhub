package com.inpetu.connect.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelefoneUtilsTest {
    @Test
    void normalizesBrazilianMobileInputToE164() {
        assertEquals("+5548999991234", TelefoneUtils.normalizarE164("(48) 99999-1234"));
        assertEquals("+5548999991234", TelefoneUtils.normalizarE164("48999991234"));
        assertEquals("+5548999991234", TelefoneUtils.normalizarE164("5548999991234"));
    }

    @Test
    void validatesAllBrazilianPhoneCases() {
        assertTrue(TelefoneUtils.e164Valido("+5548984260543"));
        assertTrue(TelefoneUtils.e164Valido("+5599984260543"));
        assertTrue(TelefoneUtils.e164Valido(null));
        assertTrue(TelefoneUtils.e164Valido("  "));

        assertFalse(TelefoneUtils.e164Valido("+5500984260543"));
        assertFalse(TelefoneUtils.e164Valido("+5520984260543"));
        assertFalse(TelefoneUtils.e164Valido("+554833335555"));
        assertFalse(TelefoneUtils.e164Valido("48984260543"));
        assertFalse(TelefoneUtils.e164Valido("telefone"));
        assertFalse(TelefoneUtils.e164Valido("+12125550100"));
    }

    @Test
    void acceptsEveryAssignedBrazilianAreaCode() {
        List<String> officialDdds = List.of(
                "11", "12", "13", "14", "15", "16", "17", "18", "19",
                "21", "22", "24", "27", "28",
                "31", "32", "33", "34", "35", "37", "38",
                "41", "42", "43", "44", "45", "46", "47", "48", "49",
                "51", "53", "54", "55",
                "61", "62", "63", "64", "65", "66", "67", "68", "69",
                "71", "73", "74", "75", "77", "79",
                "81", "82", "83", "84", "85", "86", "87", "88", "89",
                "91", "92", "93", "94", "95", "96", "97", "98", "99");
        assertEquals(67, officialDdds.size());
        for (String ddd : officialDdds) {
            assertTrue(TelefoneUtils.e164Valido("+55" + ddd + "984260543"), ddd);
        }
    }

    @Test
    void normalizesOptionalValuesAndDoesNotTreatInvalidTextAsAValidNumber() {
        assertNull(TelefoneUtils.normalizarE164(null));
        assertNull(TelefoneUtils.normalizarE164("  "));
        assertNull(TelefoneUtils.normalizarE164("telefone"));
    }
}
