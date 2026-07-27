package com.coderhan.lastmission.user.infrastructure.persistence;

import com.coderhan.lastmission.user.UserAccountLock;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JdbcUserAccountLock implements UserAccountLock {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void lockForUpdate(long userId) {
        jdbcTemplate.queryForObject("SELECT id FROM user_accounts WHERE id = ? FOR UPDATE", Long.class, userId);
    }
}
