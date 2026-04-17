package com.example.superset.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SupersetLoginResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken
) {
}
