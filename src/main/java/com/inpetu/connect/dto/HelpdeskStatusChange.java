package com.inpetu.connect.dto;

import java.time.LocalDateTime;

public record HelpdeskStatusChange(String fromStatus, String toStatus, String actorType,
                                   String actorName, String reasonCode, LocalDateTime createdAt) {
}
