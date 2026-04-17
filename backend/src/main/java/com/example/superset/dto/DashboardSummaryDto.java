package com.example.superset.dto;

public record DashboardSummaryDto(
        String id,
        String uuid,
        String embeddedId,
        String title,
        String slug
) {
}
