package com.inpetu.connect.dto;

import com.inpetu.connect.validation.TelefoneBrasileiro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroEdicaoRequest(
        @NotBlank @Size(max = 100) String nomeCompleto,
        @NotBlank @Email @Size(max = 100) String email,
        @TelefoneBrasileiro String telefone,
        @NotBlank @Size(max = 100) String organizacao,
        @NotBlank @Size(max = 100) String cargoVinculo) {
}
