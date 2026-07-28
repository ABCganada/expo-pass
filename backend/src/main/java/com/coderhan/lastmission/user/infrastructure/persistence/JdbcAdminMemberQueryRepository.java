package com.coderhan.lastmission.user.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import com.coderhan.lastmission.user.application.AdminMember;
import com.coderhan.lastmission.user.application.AdminMemberPage;
import com.coderhan.lastmission.user.application.AdminMemberQueryRepository;
import com.coderhan.lastmission.user.UserRole;
import com.coderhan.lastmission.user.domain.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcAdminMemberQueryRepository implements AdminMemberQueryRepository {
    private static final RowMapper<AccountRow> ACCOUNT_MAPPER = JdbcAdminMemberQueryRepository::mapAccount;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public AdminMemberPage findMembers(String query, int page, int size) {
        int safeSize = Math.clamp(size, 1, 100);
        int safePage = Math.max(page, 0);
        long offset = (long) safePage * safeSize;
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        long total;
        List<AccountRow> rows;
        if (term.isEmpty()) {
            total = count("SELECT count(*) FROM user_accounts");
            rows = jdbcTemplate.query("""
                    SELECT id, email, name, status FROM user_accounts
                    ORDER BY lower(coalesce(nullif(name, ''), email)), id
                    LIMIT ? OFFSET ?
                    """, ACCOUNT_MAPPER, safeSize, offset);
        } else {
            String pattern = "%" + term + "%";
            total = count("""
                    SELECT count(*) FROM user_accounts
                    WHERE lower(coalesce(name, '')) LIKE ? OR lower(email) LIKE ?
                    """, pattern, pattern);
            rows = jdbcTemplate.query("""
                    SELECT id, email, name, status FROM user_accounts
                    WHERE lower(coalesce(name, '')) LIKE ? OR lower(email) LIKE ?
                    ORDER BY lower(coalesce(nullif(name, ''), email)), id
                    LIMIT ? OFFSET ?
                    """, ACCOUNT_MAPPER, pattern, pattern, safeSize, offset);
        }

        Map<Long, EnumSet<UserRole>> rolesByUser = loadRoles(rows.stream().map(AccountRow::id).toList());
        List<AdminMember> members = rows.stream().map(row -> toMember(row, rolesByUser)).toList();
        return new AdminMemberPage(members, safePage, safeSize, total);
    }

    @Override
    public Optional<AdminMember> findById(long userId) {
        List<AccountRow> rows = jdbcTemplate.query(
                "SELECT id, email, name, status FROM user_accounts WHERE id = ?", ACCOUNT_MAPPER, userId);
        if (rows.isEmpty()) return Optional.empty();
        Map<Long, EnumSet<UserRole>> rolesByUser = loadRoles(List.of(userId));
        return Optional.of(toMember(rows.get(0), rolesByUser));
    }

    private long count(String sql, Object... arguments) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, arguments);
        return value == null ? 0L : value;
    }

    private Map<Long, EnumSet<UserRole>> loadRoles(List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        String placeholders = String.join(",", Collections.nCopies(userIds.size(), "?"));
        Map<Long, EnumSet<UserRole>> rolesByUser = new HashMap<>();
        List<Map.Entry<Long, UserRole>> pairs = jdbcTemplate.query("""
                SELECT ur.user_id AS user_id, rc.code AS code
                FROM user_roles ur
                JOIN user_role_codes rc ON rc.id = ur.role_id
                WHERE ur.user_id IN (%s)
                """.formatted(placeholders),
                (rs, rowNum) -> Map.entry(rs.getLong("user_id"), UserRole.from(rs.getString("code"))),
                userIds.toArray());
        for (Map.Entry<Long, UserRole> pair : pairs) {
            rolesByUser.computeIfAbsent(pair.getKey(), key -> EnumSet.noneOf(UserRole.class)).add(pair.getValue());
        }
        return rolesByUser;
    }

    private static AdminMember toMember(AccountRow row, Map<Long, EnumSet<UserRole>> rolesByUser) {
        EnumSet<UserRole> roles = rolesByUser.getOrDefault(row.id(), EnumSet.noneOf(UserRole.class));
        return new AdminMember(row.id(), row.email(), row.name(), row.status(), roles);
    }

    private static AccountRow mapAccount(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AccountRow(resultSet.getLong("id"), resultSet.getString("email"),
                resultSet.getString("name"), UserAccount.Status.from(resultSet.getString("status")));
    }

    private record AccountRow(long id, String email, String name, UserAccount.Status status) {
    }
}
