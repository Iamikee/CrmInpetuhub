package com.inpetu.connect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HelpdeskCreateRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 160) String subject,
    @NotBlank @Size(max = 5000) String motivo,
    @Size(max = 16) String phone,
    @Pattern(regexp = "^$|^[0-9]{1,10}$") String extension
) {
}
