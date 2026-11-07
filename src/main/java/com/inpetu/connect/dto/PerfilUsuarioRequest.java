package com.inpetu.connect.dto;

import com.inpetu.connect.util.TelefoneUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PerfilUsuarioRequest(
    @NotBlank @Size(max = 100) String nomeCompleto,
    @Size(max = 16) @Pattern(regexp = TelefoneUtils.E164_PATTERN,
        message = "Informe DDI, DDD e número válidos no formato internacional.") String telefone,
    @NotBlank @Size(max = 100) String organizacao,
    @NotBlank @Size(max = 100) String cargoVinculo
) {}
