package com.inpetu.connect.dto;

import com.inpetu.connect.validation.TelefoneBrasileiro;
import com.inpetu.connect.validation.NomeCompletoValido;
import jakarta.validation.constraints.*;

import java.util.Locale;

public record CadastroPublicoRequest(
    @NotBlank(message = "Informe nome e sobrenome")
    @Size(min = 3, max = 100, message = "O nome deve ter de 3 a 100 caracteres.")
    @Pattern(regexp = "[\\p{L}\\p{M} .'-]+", message = "Use apenas letras no nome.")
    @NomeCompletoValido
    String nomeCompleto,
    @NotBlank(message = "Informe o e-mail.")
    @Email(message = "E-mail inválido.")
    @Pattern(regexp = "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$", message = "E-mail inválido.")
    @Size(max = 100, message = "O e-mail deve ter no máximo 100 caracteres.")
    String email,
    @TelefoneBrasileiro String telefone,
    @NotBlank(message = "Informe a organização.")
    @Size(min = 2, max = 100, message = "A organização deve ter de 2 a 100 caracteres.")
    @Pattern(regexp = "(?s)(?=.*\\p{L})[\\p{L}\\p{M}\\p{N} .,;:'’&()/_+\\-#]{2,100}", message = "Informe uma organização válida.")
    String organizacao,
    @NotBlank(message = "Informe o cargo ou vínculo.")
    @Size(min = 2, max = 80, message = "O cargo ou vínculo deve ter de 2 a 80 caracteres.")
    @Pattern(regexp = "(?s)(?=(?:.*\\p{L}){2})[\\p{L}\\p{M}\\p{N} .,;:'’&()/_+\\-#]{2,80}", message = "Informe um cargo ou vínculo válido.")
    String cargoVinculo,
    @NotBlank @Size(min = 8, max = 64) String senha
) {
    public CadastroPublicoRequest {
        nomeCompleto = normalizarEspacos(nomeCompleto);
        email = email == null ? null : email.toLowerCase(Locale.ROOT);
        organizacao = apararEspacos(organizacao);
        cargoVinculo = apararEspacos(cargoVinculo);
    }

    private static String normalizarEspacos(String value) {
        return value == null ? null : apararEspacos(value).replaceAll(" {2,}", " ");
    }

    private static String apararEspacos(String value) {
        return value == null ? null : value.replaceAll("^ +| +$", "");
    }
}
