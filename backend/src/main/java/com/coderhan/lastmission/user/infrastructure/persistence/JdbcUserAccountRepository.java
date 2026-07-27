package com.coderhan.lastmission.user.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.user.application.UserAccountRepository;
import com.coderhan.lastmission.user.domain.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcUserAccountRepository implements UserAccountRepository {
    private static final RowMapper<UserAccount> USER_ROW_MAPPER = JdbcUserAccountRepository::mapUser;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<UserAccount> findByAuthSubject(String authSubject) {
        List<UserAccount> users = jdbcTemplate.query(
                "SELECT id, auth_uuid, email, name, status FROM user_accounts WHERE auth_uuid = ?",
                USER_ROW_MAPPER, authSubject);
        return users.stream().findFirst();
    }

    @Override
    public Optional<UserAccount> insertIfAbsent(String authSubject, String email, String name) {
        List<UserAccount> users = jdbcTemplate.query("""
                INSERT INTO user_accounts (auth_uuid, email, name, status)
                VALUES (?, ?, ?, 'ACTIVE')
                ON CONFLICT (auth_uuid) DO NOTHING
                RETURNING id, auth_uuid, email, name, status
                """, USER_ROW_MAPPER, authSubject, email, name);
        return users.stream().findFirst();
    }

    @Override
    public void updateProfileIfChanged(long userId, String email, String name) {
        jdbcTemplate.update("""
                UPDATE user_accounts SET email = ?, name = ?
                WHERE id = ? AND (email IS DISTINCT FROM ? OR name IS DISTINCT FROM ?)
                """, email, name, userId, email, name);
    }

    private static UserAccount mapUser(ResultSet resultSet, int rowNumber) throws SQLException {
        return new UserAccount(resultSet.getLong("id"), resultSet.getString("auth_uuid"),
                resultSet.getString("email"), resultSet.getString("name"),
                UserAccount.Status.from(resultSet.getString("status")));
    }
}
