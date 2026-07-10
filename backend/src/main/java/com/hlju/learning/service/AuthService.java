package com.hlju.learning.service;

import com.hlju.learning.domain.auth.AuthResponse;
import com.hlju.learning.domain.auth.AuthUser;
import com.hlju.learning.domain.auth.LoginRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    AuthUser toAuthUser(String userId);
}
