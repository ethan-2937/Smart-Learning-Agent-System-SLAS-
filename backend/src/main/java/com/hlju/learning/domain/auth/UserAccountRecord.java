package com.hlju.learning.domain.auth;

import java.time.Instant;
import java.util.List;

public record UserAccountRecord(
        String userId,
        String username,
        String passwordHash,
        String realName,
        List<String> roles,
        int status,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLoginAt
) {
    public boolean enabled() {
        return status == 1;
    }

    public UserAccountRecord withLastLoginAt(Instant value) {
        return new UserAccountRecord(userId, username, passwordHash, realName, roles, status, createdAt, updatedAt, value);
    }

    public UserAccountRecord withProfile(String nextUsername, String nextRealName, List<String> nextRoles,
                                         int nextStatus, Instant nextUpdatedAt) {
        return new UserAccountRecord(userId, nextUsername, passwordHash, nextRealName, nextRoles,
                nextStatus, createdAt, nextUpdatedAt, lastLoginAt);
    }

    public UserAccountRecord withPasswordHash(String nextPasswordHash, Instant nextUpdatedAt) {
        return new UserAccountRecord(userId, username, nextPasswordHash, realName, roles,
                status, createdAt, nextUpdatedAt, lastLoginAt);
    }
}
