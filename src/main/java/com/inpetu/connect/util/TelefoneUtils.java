package com.inpetu.connect.util;

import java.util.Set;

public final class TelefoneUtils {
    // DDDs assigned to Brazil under the national numbering plan.
    private static final Set<String> DDD_BRASILEIROS = Set.of(
            "11", "12", "13", "14", "15", "16", "17", "18", "19",
            "21", "22", "24", "27", "28",
            "31", "32", "33", "34", "35", "37", "38",
            "41", "42", "43", "44", "45", "46", "47", "48", "49",
            "51", "53", "54", "55",
            "61", "62", "63", "64", "65", "66", "67", "68", "69",
            "71", "73", "74", "75", "77", "79",
            "81", "82", "83", "84", "85", "86", "87", "88", "89",
            "91", "92", "93", "94", "95", "96", "97", "98", "99");

    private TelefoneUtils() {
    }

    public static String normalizarE164(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return null;
        }

        String valor = telefone.trim();
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.isEmpty()) {
            return null;
        }
        if (valor.startsWith("+")) {
            return "+" + digitos;
        }
        if (digitos.startsWith("55") && digitos.length() > 11) {
            return "+" + digitos;
        }
        return "+55" + digitos.replaceFirst("^0", "");
    }

    public static boolean e164Valido(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return true;
        }
        if (!telefone.matches("^\\+55\\d{11}$")) {
            return false;
        }

        String ddd = telefone.substring(3, 5);
        String assinante = telefone.substring(5);
        return DDD_BRASILEIROS.contains(ddd) && assinante.matches("^9\\d{8}$");
    }
}
