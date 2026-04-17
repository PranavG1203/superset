package com.example.superset.dto;

import jakarta.validation.constraints.NotBlank;

public record GuestTokenRequest(
        @NotBlank(message = "dashboardId is required")
        String dashboardId
) {
}
