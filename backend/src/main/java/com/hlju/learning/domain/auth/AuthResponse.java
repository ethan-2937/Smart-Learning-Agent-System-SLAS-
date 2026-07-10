package com.hlju.learning.domain.auth;

import java.time.Instant;

public record AuthResponse(String token, String tokenType, Instant expiresAt, AuthUser user) {
}
