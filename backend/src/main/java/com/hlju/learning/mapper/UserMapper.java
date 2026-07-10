package com.hlju.learning.mapper;

import com.hlju.learning.domain.po.UserPo;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserMapper {
    UserPo findByUsername(@Param("username") String username);

    UserPo findById(@Param("userId") String userId);

    List<UserPo> findAll(@Param("keyword") String keyword);

    void insertUser(UserPo user);

    void updateUser(UserPo user);

    void updatePassword(@Param("userId") String userId,
                        @Param("passwordHash") String passwordHash,
                        @Param("updatedAt") LocalDateTime updatedAt);

    void updateLastLoginAt(@Param("userId") String userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);
}
