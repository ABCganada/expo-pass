package com.coderhan.lastmission.user.application;

import java.util.Set;
import com.coderhan.lastmission.user.domain.RoleCode;

public interface UserRoleRepository {
    void assignDefaultRole(long userId);
    Set<RoleCode> findRoleCodes(long userId);
}
