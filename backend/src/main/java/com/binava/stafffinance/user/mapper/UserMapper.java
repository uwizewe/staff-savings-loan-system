package com.binava.stafffinance.user.mapper;

import com.binava.stafffinance.user.dto.UserView;
import com.binava.stafffinance.user.entity.AppUser;

public final class UserMapper {
    private UserMapper() {}

    public static UserView user(AppUser user) {
        return new UserView(user.id, user.username, user.fullName, user.email, user.role,
                user.enabled, user.member == null ? null : user.member.id);
    }
}
