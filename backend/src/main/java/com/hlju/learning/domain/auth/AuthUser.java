package com.hlju.learning.domain.auth;

import java.time.Instant;
import java.util.List;

public record AuthUser(
        String userId,
        String username,
        String realName,
        List<String> roles,
        Instant lastLoginAt
) {
}
