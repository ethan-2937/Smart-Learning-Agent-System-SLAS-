package com.hlju.learning.security;

import java.time.Instant;
import java.util.List;

public record JwtPrincipal(
        String userId,
        String username,
        String realName,
        List<String> roles,
        Instant issuedAt,
        Instant expiresAt
) {
}
