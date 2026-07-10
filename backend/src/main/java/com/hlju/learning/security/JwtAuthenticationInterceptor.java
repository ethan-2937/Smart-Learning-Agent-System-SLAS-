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
            if (request.getRequestURI().startsWith("/api/admin") && !user.roles().contains("ADMIN")) {
                writeForbidden(response, request, "当前账号没有管理员权限");
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
