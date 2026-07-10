package com.hlju.learning.repository.memory;

import com.hlju.learning.domain.auth.UserAccountRecord;
import com.hlju.learning.repository.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@ConditionalOnProperty(name = "app.repository.provider", havingValue = "memory", matchIfMissing = true)
public class MemoryUserRepository implements UserRepository {
    private final Map<String, UserAccountRecord> users = new ConcurrentHashMap<>();

    public MemoryUserRepository() {
        Instant now = Instant.now();
        save(new UserAccountRecord("demo-admin", "admin",
                "pbkdf2$120000$c2xhcy1hZG1pbi0yMDI2$8afB8/d+AVte0kFW4n+s6Q4xmT83Pq60QfXyAaOXrXU=",
                "System Admin", List.of("ADMIN", "TEACHER"), 1, now, now, null));
        save(new UserAccountRecord("demo-teacher", "teacher",
                "pbkdf2$120000$c2xhcy10ZWFjaGVyLTIwMjY=$11uJ/P/Mc18abzvgJdf0DugCcby3bRfRYjrRcBQMJHA=",
                "Demo Teacher", List.of("TEACHER"), 1, now, now, null));
        save(new UserAccountRecord("demo-student", "student",
                "pbkdf2$120000$c2xhcy1zdHVkZW50LTIwMjY=$sfUsjgUJCNxyvGkC+6EEhO9LEaE9gGmS8Qy69w69kik=",
                "Demo Student", List.of("STUDENT"), 1, now, now, null));
    }

    @Override
    public Optional<UserAccountRecord> findByUsername(String username) {
        return users.values().stream()
                .filter(user -> user.username().equalsIgnoreCase(username))
                .findFirst();
    }

    @Override
    public Optional<UserAccountRecord> findById(String userId) {
        return Optional.ofNullable(users.get(userId));
    }

    @Override
    public List<UserAccountRecord> findAll(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        return users.values().stream()
                .filter(user -> normalized.isBlank()
                        || user.username().toLowerCase().contains(normalized)
                        || (user.realName() != null && user.realName().toLowerCase().contains(normalized))
                        || user.roles().stream().anyMatch(role -> role.toLowerCase().contains(normalized)))
                .sorted(Comparator.comparing(UserAccountRecord::createdAt).reversed())
                .toList();
    }

    @Override
    public void save(UserAccountRecord user) {
        findByUsername(user.username())
                .filter(existing -> !existing.userId().equals(user.userId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("用户名已存在：" + user.username());
                });
        users.put(user.userId(), user);
    }

    @Override
    public void updatePassword(String userId, String passwordHash, Instant updatedAt) {
        UserAccountRecord current = users.get(userId);
        if (current != null) {
            save(current.withPasswordHash(passwordHash, updatedAt));
        }
    }

    @Override
    public void updateLastLoginAt(String userId, Instant lastLoginAt) {
        UserAccountRecord current = users.get(userId);
        if (current != null) {
            save(current.withLastLoginAt(lastLoginAt));
        }
    }

}
