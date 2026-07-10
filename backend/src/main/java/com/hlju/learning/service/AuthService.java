package com.hlju.learning.service;

import com.hlju.learning.domain.auth.AuthResponse;
import com.hlju.learning.domain.auth.AuthUser;
import com.hlju.learning.domain.auth.AdminUserRecord;
import com.hlju.learning.domain.auth.ChangePasswordRequest;
import com.hlju.learning.domain.auth.LoginRequest;
import com.hlju.learning.domain.auth.ResetPasswordRequest;
import com.hlju.learning.domain.auth.RoleOption;
import com.hlju.learning.domain.auth.UpsertUserRequest;

import java.util.List;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    AuthUser toAuthUser(String userId);

    void changePassword(AuthUser currentUser, ChangePasswordRequest request);

    List<RoleOption> listRoles(AuthUser currentUser);

    List<AdminUserRecord> listUsers(AuthUser currentUser, String keyword);

    AdminUserRecord createUser(AuthUser currentUser, UpsertUserRequest request);

    AdminUserRecord updateUser(AuthUser currentUser, String userId, UpsertUserRequest request);

    void resetPassword(AuthUser currentUser, String userId, ResetPasswordRequest request);
}
