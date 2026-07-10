package com.hlju.learning.domain.auth;

import java.time.Instant;
import java.util.List;

public record AdminUserRecord(
        String userId,
        String username,
        String realName,
        List<String> roles,
        int status,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLoginAt
) {
}
