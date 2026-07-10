package com.hlju.learning.domain.auth;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(@NotBlank String password) {
}
