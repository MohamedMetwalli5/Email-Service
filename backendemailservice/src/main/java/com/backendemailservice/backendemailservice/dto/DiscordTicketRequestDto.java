package com.backendemailservice.backendemailservice.dto;

import jakarta.validation.constraints.NotBlank;

public record DiscordTicketRequestDto(@NotBlank(message = "Ticket must not be blank") String code) {}
