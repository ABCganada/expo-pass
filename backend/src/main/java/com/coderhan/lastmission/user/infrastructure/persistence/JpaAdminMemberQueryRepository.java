package com.coderhan.lastmission.user.infrastructure.persistence;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.coderhan.lastmission.user.UserRole;
import com.coderhan.lastmission.user.application.AdminMember;
import com.coderhan.lastmission.user.application.AdminMemberPage;
import com.coderhan.lastmission.user.application.AdminMemberQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaAdminMemberQueryRepository implements AdminMemberQueryRepository {
    private final SpringDataUserAccountRepository userAccounts;

    @Override
    public AdminMemberPage findMembers(String query, int page, int size) {
        int safeSize = Math.clamp(size, 1, 100);
        int safePage = Math.max(page, 0);
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        Page<UserAccountEntity> accountPage = userAccounts.searchAll(
                term, PageRequest.of(safePage, safeSize));
        List<Long> userIds = accountPage.stream().map(UserAccountEntity::getId).toList();
        Map<Long, UserAccountEntity> accountsWithRoles = new LinkedHashMap<>();
        if (!userIds.isEmpty()) {
            userAccounts.findAllWithRolesByIdIn(userIds)
                    .forEach(user -> accountsWithRoles.put(user.getId(), user));
        }

        List<AdminMember> members = userIds.stream()
                .map(accountsWithRoles::get)
                .map(JpaAdminMemberQueryRepository::toMember)
                .toList();
        return new AdminMemberPage(
                members, safePage, safeSize, accountPage.getTotalElements());
    }

    @Override
    public Optional<AdminMember> findById(long userId) {
        return userAccounts.findWithRolesById(userId)
                .map(JpaAdminMemberQueryRepository::toMember);
    }

    private static AdminMember toMember(UserAccountEntity user) {
        Set<UserRole> roles = new LinkedHashSet<>();
        user.getRoles().stream().map(UserRoleEntity::getCode).forEach(roles::add);
        return new AdminMember(
                user.getId(), user.getEmail(), user.getName(), user.getStatus(), roles);
    }
}
