package com.coderhan.lastmission.user.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.user.application.UserAccountRepository;
import com.coderhan.lastmission.user.domain.UserAccount;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaUserAccountRepository implements UserAccountRepository {
    private final SpringDataUserAccountRepository userAccounts;
    private final EntityManager entityManager;

    @Override
    public Optional<UserAccount> findByAuthSubject(String authSubject) {
        return userAccounts.findByAuthSubject(authSubject).map(UserAccountEntity::toDomain);
    }

    @Override
    public Optional<UserAccount> insertIfAbsent(String authSubject, String email, String name) {
        // 동시 최초 로그인에서도 auth_uuid 유일성을 안전하게 지키기 위한 CockroachDB 원자적 삽입이다.
        @SuppressWarnings("unchecked")
        List<UserAccountEntity> inserted = entityManager.createNativeQuery("""
                INSERT INTO user_accounts (auth_uuid, email, name, status)
                VALUES (:authSubject, :email, :name, 'ACTIVE')
                ON CONFLICT (auth_uuid) DO NOTHING
                RETURNING id, auth_uuid, email, name, status
                """, UserAccountEntity.class)
                .setParameter("authSubject", authSubject)
                .setParameter("email", email)
                .setParameter("name", name)
                .getResultList();
        return inserted.stream().findFirst().map(UserAccountEntity::toDomain);
    }

    @Override
    public void updateProfileIfChanged(long userId, String email, String name) {
        userAccounts.findById(userId).ifPresent(user -> {
            if (!user.getEmail().equals(email) || !user.getName().equals(name)) {
                user.updateProfile(email, name);
            }
        });
    }
}
