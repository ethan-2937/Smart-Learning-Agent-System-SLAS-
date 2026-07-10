package com.hlju.learning.controller;

import com.hlju.learning.domain.auth.AuthResponse;
import com.hlju.learning.domain.auth.AuthUser;
import com.hlju.learning.domain.auth.ChangePasswordRequest;
import com.hlju.learning.domain.auth.LoginRequest;
import com.hlju.learning.security.CurrentUserHolder;
import com.hlju.learning.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/login")
    public AuthResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/api/auth/me")
    public AuthUser me() {
        return CurrentUserHolder.getRequired();
    }

    @PostMapping("/api/auth/password")
    public Map<String, Object> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        authService.changePassword(CurrentUserHolder.getRequired(), request);
        return Map.of("success", true);
    }

    @PostMapping("/api/auth/logout")
    public Map<String, Object> logout() {
        return Map.of("success", true);
    }
}
