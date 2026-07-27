package com.coderhan.lastmission.user.application;

import com.coderhan.lastmission.user.domain.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProvisioningService {
    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional
    public AuthenticatedUser provision(ProvisionUserCommand command) {
        UserAccount user = userAccountRepository.findByAuthSubject(command.authSubject())
                .orElseGet(() -> createOrLoadUser(command));
        userAccountRepository.updateProfileIfChanged(user.id(), command.email(), command.name());
        UserAccount currentUser = userAccountRepository.findByAuthSubject(command.authSubject())
                .orElseThrow(() -> new IllegalStateException("사용자 저장 후 조회할 수 없습니다."));
        return new AuthenticatedUser(currentUser, userRoleRepository.findRoleCodes(currentUser.id()));
    }

    private UserAccount createOrLoadUser(ProvisionUserCommand command) {
        return userAccountRepository.insertIfAbsent(command.authSubject(), command.email(), command.name())
                .map(createdUser -> {
                    userRoleRepository.assignDefaultRole(createdUser.id());
                    return createdUser;
                })
                .orElseGet(() -> userAccountRepository.findByAuthSubject(command.authSubject())
                        .orElseThrow(() -> new IllegalStateException("사용자 생성 후 조회할 수 없습니다.")));
    }
}
