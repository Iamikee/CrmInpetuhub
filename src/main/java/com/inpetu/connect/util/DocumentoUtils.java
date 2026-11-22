package com.inpetu.connect.util;

public final class DocumentoUtils {
    private DocumentoUtils() {}

    public static String limpar(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }

    public static boolean cpfValido(String valor) {
        String cpf = limpar(valor);
        if (cpf.length() != 11 || !cpf.matches("\\d{11}") || cpf.matches("(\\d)\\1{10}")) return false;
        int primeiro = calcularCpf(cpf, 9);
        int segundo = calcularCpf(cpf, 10);
        return cpf.charAt(9) - '0' == primeiro && cpf.charAt(10) - '0' == segundo;
    }

    public static boolean cnpjValido(String valor) {
        String cnpj = limpar(valor);
        if (cnpj.length() != 14 || !cnpj.matches("\\d{14}") || cnpj.matches("(\\d)\\1{13}")) return false;
        int primeiro = calcularCnpj(cnpj, new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int segundo = calcularCnpj(cnpj, new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return cnpj.charAt(12) - '0' == primeiro && cnpj.charAt(13) - '0' == segundo;
    }

    private static int calcularCpf(String cpf, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    private static int calcularCnpj(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (cnpj.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
