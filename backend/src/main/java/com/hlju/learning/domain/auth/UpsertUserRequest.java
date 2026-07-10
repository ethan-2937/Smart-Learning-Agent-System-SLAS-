package com.hlju.learning.domain.auth;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpsertUserRequest(
        @NotBlank String username,
        String password,
        String realName,
        List<String> roles,
        Integer status
) {
}
