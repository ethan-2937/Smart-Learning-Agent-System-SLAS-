package com.hlju.learning.repository;

import com.hlju.learning.domain.auth.UserAccountRecord;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<UserAccountRecord> findByUsername(String username);

    Optional<UserAccountRecord> findById(String userId);

    List<UserAccountRecord> findAll(String keyword);

    void save(UserAccountRecord user);

    void updatePassword(String userId, String passwordHash, Instant updatedAt);

    void updateLastLoginAt(String userId, Instant lastLoginAt);
}
