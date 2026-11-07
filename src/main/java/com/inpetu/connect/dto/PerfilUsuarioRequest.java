package com.inpetu.connect.dto;

import com.inpetu.connect.validation.TelefoneBrasileiro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PerfilUsuarioRequest(
    @NotBlank @Size(max = 100)
    @Pattern(regexp = "^\\s*\\S+(?:\\s+\\S+)+\\s*$", message = "Informe nome e sobrenome.")
    String nomeCompleto,
    @TelefoneBrasileiro String telefone,
    @NotBlank @Size(max = 100) String organizacao,
    @NotBlank @Size(max = 100) String cargoVinculo
) {}
