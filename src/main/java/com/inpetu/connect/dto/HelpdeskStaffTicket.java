package com.inpetu.connect.dto;

import java.time.LocalDateTime;
import java.util.List;

public record HelpdeskStaffTicket(
    Long id,
    String code,
    String name,
    String email,
    boolean emailVerified,
    String phone,
    String extension,
    String subject,
    String status,
    Integer assignedUserId,
    String assignedUserName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<HelpdeskPublicMessage> messages
) {
}
