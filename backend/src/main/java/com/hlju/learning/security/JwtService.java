package com.hlju.learning.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hlju.learning.config.AuthProperties;
import com.hlju.learning.domain.auth.AuthUser;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class JwtService {
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final AuthProperties properties;
    private final ObjectMapper objectMapper;

    public JwtService(AuthProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public TokenPair issue(AuthUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofHours(properties.safeTokenTtlHours()));
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = Map.of(
                "iss", properties.safeIssuer(),
                "sub", user.userId(),
                "username", user.username(),
                "realName", user.realName() == null ? user.username() : user.realName(),
                "roles", user.roles() == null ? List.of() : user.roles(),
                "iat", now.getEpochSecond(),
                "exp", expiresAt.getEpochSecond()
        );
        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String signingInput = encodedHeader + "." + encodedPayload;
        String signature = sign(signingInput);
        return new TokenPair(signingInput + "." + signature, expiresAt);
    }

    public JwtPrincipal verify(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthException("缺少登录令牌");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new AuthException("登录令牌格式错误");
        }
        String signingInput = parts[0] + "." + parts[1];
        byte[] expected = sign(signingInput).getBytes(StandardCharsets.UTF_8);
        byte[] actual = parts[2].getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new AuthException("登录令牌签名无效");
        }
        Map<String, Object> payload = decodeJson(parts[1]);
        String issuer = String.valueOf(payload.getOrDefault("iss", ""));
        if (!properties.safeIssuer().equals(issuer)) {
            throw new AuthException("登录令牌签发方无效");
        }
        Instant expiresAt = Instant.ofEpochSecond(asLong(payload.get("exp")));
        if (expiresAt.isBefore(Instant.now())) {
            throw new AuthException("登录已过期，请重新登录");
        }
        Instant issuedAt = Instant.ofEpochSecond(asLong(payload.get("iat")));
        List<String> roles = objectMapper.convertValue(payload.getOrDefault("roles", List.of()), new TypeReference<List<String>>() {});
        return new JwtPrincipal(String.valueOf(payload.get("sub")), String.valueOf(payload.get("username")),
                String.valueOf(payload.getOrDefault("realName", "")), roles, issuedAt, expiresAt);
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("JWT encode failed", ex);
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            byte[] json = BASE64_URL_DECODER.decode(value);
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new AuthException("登录令牌内容无效");
        }
    }

    private String sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(properties.safeJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("JWT sign failed", ex);
        }
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    public record TokenPair(String token, Instant expiresAt) {
    }
}
