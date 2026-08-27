package com.coderhan.lastmission.user.infrastructure.persistence;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.coderhan.lastmission.user.UserRole;
import com.coderhan.lastmission.user.application.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaUserRoleRepository implements UserRoleRepository {
    private final SpringDataUserAccountRepository userAccounts;
    private final SpringDataUserRoleRepository roleCodes;

    @Override
    public void assignDefaultRole(long userId) {
        UserAccountEntity user = userAccounts.findWithRolesById(userId)
                .orElseThrow(() -> new IllegalStateException("기본 권한을 부여할 사용자를 찾을 수 없습니다."));
        UserRoleEntity defaultRole = roleCodes.findByCode(UserRole.USER)
                .orElseThrow(() -> new IllegalStateException("기본 USER 역할 코드가 없습니다."));
        user.addRole(defaultRole);
    }

    @Override
    public Set<UserRole> findRoleCodes(long userId) {
        return userAccounts.findWithRolesById(userId)
                .map(user -> {
                    Set<UserRole> roles = new LinkedHashSet<>();
                    user.getRoles().stream().map(UserRoleEntity::getCode).forEach(roles::add);
                    return roles;
                })
                .orElseGet(Set::of);
    }

    @Override
    public void replaceRoles(long userId, Set<UserRole> roles) {
        UserAccountEntity user = userAccounts.findWithRolesById(userId)
                .orElseThrow(() -> new IllegalStateException("권한을 변경할 사용자를 찾을 수 없습니다."));
        List<UserRoleEntity> resolvedRoles = roleCodes.findByCodeIn(roles);
        if (resolvedRoles.size() != roles.size()) {
            throw new IllegalStateException("존재하지 않는 사용자 권한이 포함되어 있습니다.");
        }
        user.replaceRoles(new LinkedHashSet<>(resolvedRoles));
    }
}
