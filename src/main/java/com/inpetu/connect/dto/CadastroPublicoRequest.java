package com.inpetu.connect.dto;

import com.inpetu.connect.util.TelefoneUtils;
import jakarta.validation.constraints.*;

public record CadastroPublicoRequest(
    @NotBlank @Size(max = 100) String nomeCompleto,
    @NotBlank @Email @Size(max = 100) String email,
    @Size(max = 16) @Pattern(regexp = TelefoneUtils.E164_PATTERN,
        message = "Informe DDI, DDD e número válidos no formato internacional.") String telefone,
    @NotBlank @Size(max = 100) String organizacao,
    @NotBlank @Size(max = 100) String cargoVinculo,
    @NotBlank @Size(min = 8, max = 64) String senha
) {}
