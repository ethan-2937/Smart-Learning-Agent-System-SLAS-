package com.hlju.learning.serviceimpl;

import com.hlju.learning.domain.auth.AuthResponse;
import com.hlju.learning.domain.auth.AuthUser;
import com.hlju.learning.domain.auth.LoginRequest;
import com.hlju.learning.domain.auth.UserAccountRecord;
import com.hlju.learning.repository.UserRepository;
import com.hlju.learning.security.AuthException;
import com.hlju.learning.security.JwtService;
import com.hlju.learning.security.PasswordHasher;
import com.hlju.learning.service.AuthService;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthServiceImpl implements AuthService {
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
                .orElseThrow(() -> new AuthException("用户名或密码错误"));
        if (!user.enabled()) {
            throw new AuthException("账号已停用，请联系管理员");
        }
        if (!passwordHasher.matches(request.password(), user.passwordHash())) {
            throw new AuthException("用户名或密码错误");
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
                .orElseThrow(() -> new AuthException("登录用户不存在"));
        if (!user.enabled()) {
            throw new AuthException("账号已停用，请联系管理员");
        }
        return toAuthUser(user);
    }

    private AuthUser toAuthUser(UserAccountRecord user) {
        return new AuthUser(user.userId(), user.username(), user.realName(), user.roles(), user.lastLoginAt());
    }
}
