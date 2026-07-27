package com.coderhan.lastmission.user.application;

import java.util.Optional;
import com.coderhan.lastmission.user.domain.UserAccount;

public interface UserAccountRepository {
    Optional<UserAccount> findByAuthSubject(String authSubject);
    Optional<UserAccount> insertIfAbsent(String authSubject, String email, String name);
    void updateProfileIfChanged(long userId, String email, String name);
}
