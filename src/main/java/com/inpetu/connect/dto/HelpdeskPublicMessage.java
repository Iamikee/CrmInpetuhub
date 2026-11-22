package com.inpetu.connect.dto;

import java.time.LocalDateTime;

public record HelpdeskPublicMessage(long seq, String authorType, String authorName,
                                    String body, LocalDateTime createdAt) {
}
