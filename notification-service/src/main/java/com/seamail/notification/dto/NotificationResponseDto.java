package com.seamail.notification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record NotificationResponseDto(
        Long id,
        String type,
        Long sourceEmailId,
        String subjectSnapshot,
        String senderSnapshot,
        boolean read,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {}