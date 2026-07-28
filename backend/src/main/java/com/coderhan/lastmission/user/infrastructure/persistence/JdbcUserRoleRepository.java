package com.coderhan.lastmission.user.infrastructure.persistence;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.coderhan.lastmission.user.application.UserRoleRepository;
import com.coderhan.lastmission.user.UserRole;
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
    public void replaceRoles(long userId, Set<UserRole> roles) {
        List<String> codes = roles.stream().map(Enum::name).toList();
        // 요청에 없는 권한 제거. (roles 는 항상 USER 를 포함하므로 전부 지워지지 않는다.)
        String placeholders = String.join(",", Collections.nCopies(codes.size(), "?"));
        Object[] deleteArguments = new Object[codes.size() + 1];
        deleteArguments[0] = userId;
        for (int index = 0; index < codes.size(); index++) deleteArguments[index + 1] = codes.get(index);
        jdbcTemplate.update("""
                DELETE FROM user_roles
                WHERE user_id = ?
                  AND role_id NOT IN (SELECT id FROM user_role_codes WHERE code IN (%s))
                """.formatted(placeholders), deleteArguments);
        // 없는 권한 추가.
        for (String code : codes) {
            jdbcTemplate.update("""
                    INSERT INTO user_roles (user_id, role_id)
                    SELECT ?, id FROM user_role_codes WHERE code = ?
                    ON CONFLICT (user_id, role_id) DO NOTHING
                    """, userId, code);
        }
    }

    @Override
    public Set<UserRole> findRoleCodes(long userId) {
        Set<UserRole> roles = new LinkedHashSet<>();
        jdbcTemplate.queryForList("""
                SELECT r.code FROM user_role_codes r
                JOIN user_roles ur ON ur.role_id = r.id
                WHERE ur.user_id = ? ORDER BY r.code
                """, String.class, userId).stream().map(UserRole::from).forEach(roles::add);
        return roles;
    }
}
