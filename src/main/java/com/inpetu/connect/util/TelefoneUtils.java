package com.inpetu.connect.util;

public final class TelefoneUtils {
    public static final String E164_PATTERN =
            "^$|\\+(?:55|351|44|54|56|57|52|34|33|49|39|61|81|91|1)\\d{6,12}$";

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
        return telefone == null || telefone.isBlank()
                || telefone.matches(E164_PATTERN);
    }
}
