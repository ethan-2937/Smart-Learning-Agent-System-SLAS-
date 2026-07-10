package com.hlju.learning.mapper;

import com.hlju.learning.domain.po.UserPo;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface UserMapper {
    UserPo findByUsername(@Param("username") String username);

    UserPo findById(@Param("userId") String userId);

    void updateLastLoginAt(@Param("userId") String userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);
}
