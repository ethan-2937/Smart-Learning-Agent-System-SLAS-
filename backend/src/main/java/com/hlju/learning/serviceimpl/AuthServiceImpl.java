package com.hlju.learning.serviceimpl;

import com.hlju.learning.domain.auth.AdminUserRecord;
import com.hlju.learning.domain.auth.AuthResponse;
import com.hlju.learning.domain.auth.AuthUser;
import com.hlju.learning.domain.auth.ChangePasswordRequest;
import com.hlju.learning.domain.auth.LoginRequest;
import com.hlju.learning.domain.auth.ResetPasswordRequest;
import com.hlju.learning.domain.auth.RoleOption;
import com.hlju.learning.domain.auth.UpsertUserRequest;
import com.hlju.learning.domain.auth.UserAccountRecord;
import com.hlju.learning.repository.UserRepository;
import com.hlju.learning.security.AuthException;
import com.hlju.learning.security.JwtService;
import com.hlju.learning.security.PasswordHasher;
import com.hlju.learning.service.AuthService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {
    private static final List<RoleOption> ROLE_OPTIONS = List.of(
            new RoleOption("ADMIN", "?????", "?????????????"),
            new RoleOption("TEACHER", "??", "????????????????"),
            new RoleOption("STUDENT", "??", "??????????????")
    );

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository, PasswordHasher passwordHasher, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.jwtService = jwtService;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        UserAccountRecord user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(() -> new AuthException("????????"));
        if (!user.enabled()) {
            throw new AuthException("????????????");
        }
        if (!passwordHasher.matches(request.password(), user.passwordHash())) {
            throw new AuthException("????????");
        }
        Instant now = Instant.now();
        userRepository.updateLastLoginAt(user.userId(), now);
        AuthUser authUser = toAuthUser(user.withLastLoginAt(now));
        JwtService.TokenPair token = jwtService.issue(authUser);
        return new AuthResponse(token.token(), "Bearer", token.expiresAt(), authUser);
    }

    @Override
    public AuthUser toAuthUser(String userId) {
        UserAccountRecord user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("???????"));
        if (!user.enabled()) {
            throw new AuthException("????????????");
        }
        return toAuthUser(user);
    }

    @Override
    public void changePassword(AuthUser currentUser, ChangePasswordRequest request) {
        UserAccountRecord user = userRepository.findById(currentUser.userId())
                .orElseThrow(() -> new AuthException("???????"));
        if (!passwordHasher.matches(request.oldPassword(), user.passwordHash())) {
            throw new AuthException("???????");
        }
        ensurePassword(request.newPassword());
        userRepository.updatePassword(user.userId(), passwordHasher.hash(request.newPassword()), Instant.now());
    }

    @Override
    public List<RoleOption> listRoles(AuthUser currentUser) {
        requireAdmin(currentUser);
        return ROLE_OPTIONS;
    }

    @Override
    public List<AdminUserRecord> listUsers(AuthUser currentUser, String keyword) {
        requireAdmin(currentUser);
        return userRepository.findAll(keyword).stream().map(this::toAdminRecord).toList();
    }

    @Override
    public AdminUserRecord createUser(AuthUser currentUser, UpsertUserRequest request) {
        requireAdmin(currentUser);
        String username = normalizeUsername(request.username());
        userRepository.findByUsername(username).ifPresent(existing -> {
            throw new IllegalArgumentException("???????" + username);
        });
        ensurePassword(request.password());
        Instant now = Instant.now();
        UserAccountRecord user = new UserAccountRecord(UUID.randomUUID().toString(), username,
                passwordHasher.hash(request.password()), blankToNull(request.realName()), normalizeRoles(request.roles()),
                normalizeStatus(request.status()), now, now, null);
        userRepository.save(user);
        return toAdminRecord(user);
    }

    @Override
    public AdminUserRecord updateUser(AuthUser currentUser, String userId, UpsertUserRequest request) {
        requireAdmin(currentUser);
        String username = normalizeUsername(request.username());
        UserAccountRecord current = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("??????" + userId));
        userRepository.findByUsername(username)
                .filter(existing -> !existing.userId().equals(userId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("???????" + username);
                });
        UserAccountRecord updated = current.withProfile(username, blankToNull(request.realName()),
                normalizeRoles(request.roles()), normalizeStatus(request.status()), Instant.now());
        userRepository.save(updated);
        return toAdminRecord(updated);
    }

    @Override
    public void resetPassword(AuthUser currentUser, String userId, ResetPasswordRequest request) {
        requireAdmin(currentUser);
        ensurePassword(request.password());
        userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("??????" + userId));
        userRepository.updatePassword(userId, passwordHasher.hash(request.password()), Instant.now());
    }

    private AuthUser toAuthUser(UserAccountRecord user) {
        return new AuthUser(user.userId(), user.username(), user.realName(), user.roles(), user.lastLoginAt());
    }

    private AdminUserRecord toAdminRecord(UserAccountRecord user) {
        return new AdminUserRecord(user.userId(), user.username(), user.realName(), user.roles(), user.status(),
                user.createdAt(), user.updatedAt(), user.lastLoginAt());
    }

    private void requireAdmin(AuthUser user) {
        if (user == null || user.roles() == null || !user.roles().contains("ADMIN")) {
            throw new AuthException("??????????????");
        }
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("???????");
        }
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_.-]{3,32}")) {
            throw new IllegalArgumentException("????? 3-32 ????????????????");
        }
        return normalized;
    }

    private List<String> normalizeRoles(List<String> roles) {
        List<String> normalized = roles == null || roles.isEmpty()
                ? List.of("STUDENT")
                : roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        List<String> supported = ROLE_OPTIONS.stream().map(RoleOption::roleCode).toList();
        if (normalized.stream().anyMatch(role -> !supported.contains(role))) {
            throw new IllegalArgumentException("?????????");
        }
        return normalized;
    }

    private int normalizeStatus(Integer status) {
        return status != null && status == 0 ? 0 : 1;
    }

    private void ensurePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("?????? 6 ?");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
