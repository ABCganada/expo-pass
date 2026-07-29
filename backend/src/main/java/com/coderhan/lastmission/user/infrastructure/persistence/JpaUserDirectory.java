package com.coderhan.lastmission.user.infrastructure.persistence;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import com.coderhan.lastmission.user.UserAccess;
import com.coderhan.lastmission.user.UserDirectory;
import com.coderhan.lastmission.user.UserRef;
import com.coderhan.lastmission.user.UserRole;
import com.coderhan.lastmission.user.domain.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaUserDirectory implements UserDirectory {
    private final SpringDataUserAccountRepository userAccounts;

    @Override
    public Optional<UserAccess> findActiveAccessById(long userId) {
        return userAccounts.findWithRolesByIdAndStatus(userId, UserAccount.Status.ACTIVE)
                .map(user -> new UserAccess(user.getId(), roleCodes(user)));
    }

    @Override
    public Optional<UserRef> findActiveByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return userAccounts.findByEmailIgnoreCaseAndStatus(email.trim(), UserAccount.Status.ACTIVE)
                .map(JpaUserDirectory::toRef);
    }

    @Override
    public List<UserRef> findActiveByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        List<Long> distinctIds = userIds.stream().distinct().toList();
        return userAccounts.findByIdInAndStatus(distinctIds, UserAccount.Status.ACTIVE).stream()
                .map(JpaUserDirectory::toRef)
                .toList();
    }

    @Override
    public List<UserRef> searchActive(String query, int limit) {
        int safeLimit = Math.clamp(limit, 1, 100);
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return userAccounts.searchByStatus(
                        UserAccount.Status.ACTIVE, term, PageRequest.of(0, safeLimit)).stream()
                .map(JpaUserDirectory::toRef)
                .toList();
    }

    private static UserRef toRef(UserAccountEntity user) {
        return new UserRef(user.getId(), user.getEmail(), user.getName());
    }

    private static Set<UserRole> roleCodes(UserAccountEntity user) {
        Set<UserRole> roles = new LinkedHashSet<>();
        user.getRoles().stream().map(UserRoleEntity::getCode).forEach(roles::add);
        return roles;
    }
}
