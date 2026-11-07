package com.inpetu.connect.dto;

import com.inpetu.connect.validation.TelefoneBrasileiro;
import jakarta.validation.constraints.*;

public record DuvidaRequest(
    @NotBlank @Email @Size(max = 100) String emailContato,
    @TelefoneBrasileiro String telefoneContato,
    @NotBlank @Size(max = 5000) String mensagemDuvida
) {}
