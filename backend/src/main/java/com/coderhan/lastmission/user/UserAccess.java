package com.coderhan.lastmission.user;

import java.util.Set;

/** 다른 모듈에 공개하는 활성 사용자 식별자와 전체 권한. */
public record UserAccess(long userId, Set<UserRole> roles) {
    public UserAccess {
        roles = Set.copyOf(roles);
    }
}
