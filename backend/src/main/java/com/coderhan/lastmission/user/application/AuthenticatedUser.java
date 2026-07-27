package com.coderhan.lastmission.user.application;

import java.util.Set;
import com.coderhan.lastmission.user.domain.RoleCode;
import com.coderhan.lastmission.user.domain.UserAccount;

public record AuthenticatedUser(UserAccount user, Set<RoleCode> roles) {
    public AuthenticatedUser {
        roles = Set.copyOf(roles);
    }
}
