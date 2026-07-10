package com.hlju.learning.repository;

import com.hlju.learning.domain.auth.UserAccountRecord;

import java.time.Instant;
import java.util.Optional;

public interface UserRepository {
    Optional<UserAccountRecord> findByUsername(String username);

    Optional<UserAccountRecord> findById(String userId);

    void updateLastLoginAt(String userId, Instant lastLoginAt);
}
