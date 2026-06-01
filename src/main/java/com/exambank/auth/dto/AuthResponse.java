package com.exambank.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UUID userId,
        String email) {

    public static AuthResponse bearer(String accessToken, UUID userId, String email) {
        return new AuthResponse(accessToken, "Bearer", userId, email);
    }
}