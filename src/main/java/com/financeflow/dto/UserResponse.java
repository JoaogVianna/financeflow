package com.financeflow.dto;

import com.financeflow.entity.User;

// Nunca devolvemos a senha (nem o hash) pela API.
public record UserResponse(Long id, String name, String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
