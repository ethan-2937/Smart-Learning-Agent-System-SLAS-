package com.hlju.learning.repository.mybatis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hlju.learning.domain.auth.UserAccountRecord;
import com.hlju.learning.domain.po.UserPo;
import com.hlju.learning.mapper.UserMapper;
import com.hlju.learning.repository.UserRepository;
import com.hlju.learning.util.JsonCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.repository.provider", havingValue = "mybatis")
public class MyBatisUserRepository implements UserRepository {
    private final UserMapper mapper;
    private final JsonCodec jsonCodec;

    public MyBatisUserRepository(UserMapper mapper, JsonCodec jsonCodec) {
        this.mapper = mapper;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public Optional<UserAccountRecord> findByUsername(String username) {
        return Optional.ofNullable(mapper.findByUsername(username)).map(this::toDomain);
    }

    @Override
    public Optional<UserAccountRecord> findById(String userId) {
        return Optional.ofNullable(mapper.findById(userId)).map(this::toDomain);
    }

    @Override
    public void updateLastLoginAt(String userId, Instant lastLoginAt) {
        mapper.updateLastLoginAt(userId, toLocal(lastLoginAt));
    }

    private UserAccountRecord toDomain(UserPo po) {
        return new UserAccountRecord(po.getUserId(), po.getUsername(), po.getPasswordHash(), po.getRealName(),
                jsonCodec.readList(po.getRolesJson(), new TypeReference<List<String>>() {}),
                po.getStatus() == null ? 0 : po.getStatus(), toInstant(po.getCreatedAt()), toInstant(po.getUpdatedAt()),
                po.getLastLoginAt() == null ? null : toInstant(po.getLastLoginAt()));
    }

    private LocalDateTime toLocal(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? Instant.now() : value.toInstant(ZoneOffset.UTC);
    }
}
