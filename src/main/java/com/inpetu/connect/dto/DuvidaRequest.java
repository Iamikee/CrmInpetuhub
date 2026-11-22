package com.inpetu.connect.dto;

import com.inpetu.connect.validation.TelefoneBrasileiro;
import jakarta.validation.constraints.*;

import java.util.Locale;

public record DuvidaRequest(
    @NotBlank(message = "Informe o e-mail.")
    @Email(message = "E-mail inválido.")
    @Pattern(regexp = "^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$", message = "E-mail inválido.")
    @Size(max = 100, message = "O e-mail deve ter no máximo 100 caracteres.")
    String emailContato,
    @TelefoneBrasileiro String telefoneContato,
    @NotBlank @Size(max = 5000) String mensagemDuvida
) {
    public DuvidaRequest {
        emailContato = emailContato == null ? null : emailContato.toLowerCase(Locale.ROOT);
    }
}
