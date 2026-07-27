package com.coderhan.lastmission.user.infrastructure.persistence;

import java.util.LinkedHashSet;
import java.util.Set;
import com.coderhan.lastmission.user.application.UserRoleRepository;
import com.coderhan.lastmission.user.domain.RoleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcUserRoleRepository implements UserRoleRepository {
    private static final String DEFAULT_ROLE = "USER";
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void assignDefaultRole(long userId) {
        int assignedRows = jdbcTemplate.update("""
                INSERT INTO user_roles (user_id, role_id)
                SELECT ?, id FROM user_role_codes WHERE code = ?
                ON CONFLICT (user_id, role_id) DO NOTHING
                """, userId, DEFAULT_ROLE);
        if (assignedRows != 1) throw new IllegalStateException("기본 USER 역할을 사용자에게 할당할 수 없습니다.");
    }

    @Override
    public Set<RoleCode> findRoleCodes(long userId) {
        Set<RoleCode> roles = new LinkedHashSet<>();
        jdbcTemplate.queryForList("""
                SELECT r.code FROM user_role_codes r
                JOIN user_roles ur ON ur.role_id = r.id
                WHERE ur.user_id = ? ORDER BY r.code
                """, String.class, userId).stream().map(RoleCode::from).forEach(roles::add);
        return roles;
    }
}
