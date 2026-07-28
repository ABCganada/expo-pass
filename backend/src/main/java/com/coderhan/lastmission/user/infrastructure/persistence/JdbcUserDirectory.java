package com.coderhan.lastmission.user.infrastructure.persistence;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import com.coderhan.lastmission.user.UserAccess;
import com.coderhan.lastmission.user.UserDirectory;
import com.coderhan.lastmission.user.UserRef;
import com.coderhan.lastmission.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcUserDirectory implements UserDirectory {
    private final JdbcTemplate jdbcTemplate;
    private static final org.springframework.jdbc.core.RowMapper<UserRef> USER_MAPPER =
            (rs, rowNum) -> new UserRef(
                    rs.getLong("id"), rs.getString("email"), rs.getString("name"));

    @Override
    public Optional<UserAccess> findActiveAccessById(long userId) {
        return jdbcTemplate.query("""
                SELECT u.id, r.code
                FROM user_accounts u
                LEFT JOIN user_roles ur ON ur.user_id = u.id
                LEFT JOIN user_role_codes r ON r.id = ur.role_id
                WHERE u.id = ? AND u.status = 'ACTIVE'
                ORDER BY r.code
                """, resultSet -> {
            if (!resultSet.next()) return Optional.empty();

            long activeUserId = resultSet.getLong("id");
            EnumSet<UserRole> roles = EnumSet.noneOf(UserRole.class);
            do {
                String role = resultSet.getString("code");
                if (role != null) roles.add(UserRole.from(role));
            } while (resultSet.next());

            return Optional.of(new UserAccess(activeUserId, roles));
        }, userId);
    }

    @Override
    public Optional<UserRef> findActiveByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        List<UserRef> found = jdbcTemplate.query("""
                SELECT id, email, name FROM user_accounts
                WHERE lower(email) = lower(?) AND status = 'ACTIVE'
                """, USER_MAPPER, email.trim());
        return found.stream().findFirst();
    }

    @Override
    public List<UserRef> findActiveByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        List<Long> distinctIds = userIds.stream().distinct().toList();
        String placeholders = String.join(",", Collections.nCopies(distinctIds.size(), "?"));
        return jdbcTemplate.query("""
                SELECT id, email, name FROM user_accounts
                WHERE status = 'ACTIVE' AND id IN (%s)
                """.formatted(placeholders), USER_MAPPER, distinctIds.toArray());
    }

    @Override
    public List<UserRef> searchActive(String query, int limit) {
        int safeLimit = Math.clamp(limit, 1, 100);
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (term.isEmpty()) {
            return jdbcTemplate.query("""
                    SELECT id, email, name FROM user_accounts
                    WHERE status = 'ACTIVE'
                    ORDER BY lower(coalesce(nullif(name, ''), email)), id
                    LIMIT ?
                    """, USER_MAPPER, safeLimit);
        }
        String pattern = "%" + term + "%";
        return jdbcTemplate.query("""
                SELECT id, email, name FROM user_accounts
                WHERE status = 'ACTIVE'
                  AND (lower(coalesce(name, '')) LIKE ? OR lower(email) LIKE ?)
                ORDER BY lower(coalesce(nullif(name, ''), email)), id
                LIMIT ?
                """, USER_MAPPER, pattern, pattern, safeLimit);
    }
}
