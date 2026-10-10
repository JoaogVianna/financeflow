package com.financeflow.dto;

public record LoginResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {
}
