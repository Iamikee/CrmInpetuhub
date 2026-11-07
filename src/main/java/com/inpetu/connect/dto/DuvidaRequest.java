package com.inpetu.connect.dto;

import com.inpetu.connect.util.TelefoneUtils;
import jakarta.validation.constraints.*;

public record DuvidaRequest(
    @NotBlank @Email @Size(max = 100) String emailContato,
    @Size(max = 16) @Pattern(regexp = TelefoneUtils.E164_PATTERN,
        message = "Informe DDI, DDD e número válidos no formato internacional.") String telefoneContato,
    @NotBlank @Size(max = 5000) String mensagemDuvida
) {}
