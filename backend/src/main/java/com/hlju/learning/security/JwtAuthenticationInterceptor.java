package com.hlju.learning.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hlju.learning.common.ApiError;
import com.hlju.learning.domain.auth.AuthUser;
import com.hlju.learning.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.List;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {
    private final JwtService jwtService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationInterceptor(JwtService jwtService, AuthService authService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublicApi(request.getRequestURI())) {
            return true;
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeUnauthorized(response, request, "请先登录");
            return false;
        }
        try {
            JwtPrincipal principal = jwtService.verify(authorization.substring("Bearer ".length()));
            AuthUser user = authService.toAuthUser(principal.userId());
            if (!isAllowed(request, user)) {
                writeForbidden(response, request, "当前账号没有对应接口权限");
                return false;
            }
            CurrentUserHolder.set(user);
            return true;
        } catch (AuthException ex) {
            writeUnauthorized(response, request, ex.getMessage());
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserHolder.clear();
    }

    private boolean isPublicApi(String uri) {
        return uri.equals("/api/auth/login")
                || uri.equals("/api/auth/logout")
                || uri.equals("/api/health");
    }

    private boolean isAllowed(HttpServletRequest request, AuthUser user) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if (uri.startsWith("/api/admin")) {
            return hasRole(user, "ADMIN");
        }
        if (uri.startsWith("/api/auth")) {
            return true;
        }
        if (uri.startsWith("/api/practice") || uri.startsWith("/api/runtime")) {
            return true;
        }
        if (uri.startsWith("/api/materials") && method.equalsIgnoreCase("GET")) {
            return hasAnyRole(user, "ADMIN", "TEACHER", "STUDENT");
        }
        if (uri.startsWith("/api/courses") && method.equalsIgnoreCase("GET")) {
            return hasAnyRole(user, "ADMIN", "TEACHER", "STUDENT");
        }
        if (uri.startsWith("/api/knowledge-points") && method.equalsIgnoreCase("GET")) {
            return hasAnyRole(user, "ADMIN", "TEACHER", "STUDENT");
        }
        return hasAnyRole(user, "ADMIN", "TEACHER");
    }

    private boolean hasAnyRole(AuthUser user, String... roles) {
        for (String role : roles) {
            if (hasRole(user, role)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRole(AuthUser user, String role) {
        List<String> roles = user.roles() == null ? List.of() : user.roles();
        return roles.contains(role);
    }

    private void writeUnauthorized(HttpServletResponse response, HttpServletRequest request, String message) throws Exception {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ApiError(message, request.getRequestURI(), Instant.now()));
    }

    private void writeForbidden(HttpServletResponse response, HttpServletRequest request, String message) throws Exception {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ApiError(message, request.getRequestURI(), Instant.now()));
    }
}
