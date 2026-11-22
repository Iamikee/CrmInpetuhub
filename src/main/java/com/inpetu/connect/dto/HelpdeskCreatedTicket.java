package com.inpetu.connect.dto;

import java.time.LocalDateTime;

public record HelpdeskCreatedTicket(String code, String token, String status, LocalDateTime createdAt) {
}
