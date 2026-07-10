package com.hlju.learning.security;

import com.hlju.learning.domain.auth.AuthUser;

public final class CurrentUserHolder {
    private static final ThreadLocal<AuthUser> CURRENT = new ThreadLocal<>();

    private CurrentUserHolder() {
    }

    public static void set(AuthUser user) {
        CURRENT.set(user);
    }

    public static AuthUser getRequired() {
        AuthUser user = CURRENT.get();
        if (user == null) {
            throw new AuthException("请先登录");
        }
        return user;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
