package com.example.superset.dto;

import java.util.List;

public record SupersetGuestTokenApiRequest(
        List<Resource> resources,
        User user,
        List<Object> rls
) {
    public record Resource(String type, String id) {
    }

    public record User(String username, String first_name, String last_name) {
    }
}
