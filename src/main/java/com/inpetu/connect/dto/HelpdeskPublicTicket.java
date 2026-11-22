package com.inpetu.connect.dto;

import java.time.LocalDateTime;
import java.util.List;

public record HelpdeskPublicTicket(String code, String subject, String status,
                                   LocalDateTime createdAt, LocalDateTime updatedAt,
                                   List<HelpdeskPublicMessage> messages) {
}
