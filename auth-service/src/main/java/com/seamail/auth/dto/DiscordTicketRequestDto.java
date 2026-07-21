package com.seamail.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record DiscordTicketRequestDto(@NotBlank(message = "Ticket must not be blank") String code) {}
