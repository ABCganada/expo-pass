package com.coderhan.lastmission.user.application;

import java.util.Set;
import com.coderhan.lastmission.user.domain.RoleCode;

public interface UserRoleRepository {
    void assignDefaultRole(long userId);
    Set<RoleCode> findRoleCodes(long userId);

    /**
     * 사용자의 권한을 주어진 집합으로 교체한다(없는 건 추가, 빠진 건 제거).
     * 원자성은 호출부의 트랜잭션 경계가 보장한다.
     */
    void replaceRoles(long userId, Set<RoleCode> roles);
}
