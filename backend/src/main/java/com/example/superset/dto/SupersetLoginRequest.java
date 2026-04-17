package com.example.superset.dto;

public record SupersetLoginRequest(
        String username,
        String password,
        String provider,
        boolean refresh
) {
}
