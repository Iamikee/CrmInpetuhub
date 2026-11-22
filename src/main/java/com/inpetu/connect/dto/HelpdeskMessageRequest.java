package com.inpetu.connect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HelpdeskMessageRequest(
    @NotBlank @Size(max = 5000) String body
) {
}
