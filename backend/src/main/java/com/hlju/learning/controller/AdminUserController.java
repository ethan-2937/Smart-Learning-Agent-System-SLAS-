package com.hlju.learning.controller;

import com.hlju.learning.domain.auth.AdminUserRecord;
import com.hlju.learning.domain.auth.ResetPasswordRequest;
import com.hlju.learning.domain.auth.RoleOption;
import com.hlju.learning.domain.auth.UpsertUserRequest;
import com.hlju.learning.security.CurrentUserHolder;
import com.hlju.learning.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AdminUserController {
    private final AuthService authService;

    public AdminUserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/api/admin/roles")
    public List<RoleOption> listRoles() {
        return authService.listRoles(CurrentUserHolder.getRequired());
    }

    @GetMapping("/api/admin/users")
    public List<AdminUserRecord> listUsers(@RequestParam(value = "keyword", required = false) String keyword) {
        return authService.listUsers(CurrentUserHolder.getRequired(), keyword);
    }

    @PostMapping("/api/admin/users")
    public AdminUserRecord createUser(@RequestBody @Valid UpsertUserRequest request) {
        return authService.createUser(CurrentUserHolder.getRequired(), request);
    }

    @PutMapping("/api/admin/users/{userId}")
    public AdminUserRecord updateUser(@PathVariable String userId, @RequestBody @Valid UpsertUserRequest request) {
        return authService.updateUser(CurrentUserHolder.getRequired(), userId, request);
    }

    @PostMapping("/api/admin/users/{userId}/password")
    public Map<String, Object> resetPassword(@PathVariable String userId,
                                             @RequestBody @Valid ResetPasswordRequest request) {
        authService.resetPassword(CurrentUserHolder.getRequired(), userId, request);
        return Map.of("success", true);
    }
}
