package com.financeflow.security;

import org.springframework.security.oauth2.jwt.Jwt;

/** Extrai o id do usuário logado a partir do token JWT (o "sub"). */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
