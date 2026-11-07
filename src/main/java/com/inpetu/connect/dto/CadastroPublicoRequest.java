package com.inpetu.connect.dto;

import com.inpetu.connect.validation.TelefoneBrasileiro;
import jakarta.validation.constraints.*;

public record CadastroPublicoRequest(
    @NotBlank @Size(max = 100) String nomeCompleto,
    @NotBlank @Email @Size(max = 100) String email,
    @TelefoneBrasileiro String telefone,
    @NotBlank @Size(max = 100) String organizacao,
    @NotBlank @Size(max = 100) String cargoVinculo,
    @NotBlank @Size(min = 8, max = 64) String senha
) {}
