package com.coderhan.lastmission.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.Set;
import com.coderhan.lastmission.user.UserRole;
import com.coderhan.lastmission.user.domain.UserAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceTest {
    @Mock UserAccountRepository userAccountRepository;
    @Mock UserRoleRepository userRoleRepository;
    @InjectMocks UserProvisioningService service;

    @Test
    void createsNewUserAndAssignsDefaultRole() {
        ProvisionUserCommand command = new ProvisionUserCommand("keycloak-subject", "user@example.com", "사용자");
        UserAccount created = user("user@example.com", "사용자");
        when(userAccountRepository.findByAuthSubject("keycloak-subject"))
                .thenReturn(Optional.empty(), Optional.of(created));
        when(userAccountRepository.insertIfAbsent("keycloak-subject", "user@example.com", "사용자"))
                .thenReturn(Optional.of(created));
        when(userRoleRepository.findRoleCodes(10L)).thenReturn(Set.of(UserRole.USER));

        AuthenticatedUser result = service.provision(command);

        verify(userRoleRepository).assignDefaultRole(10L);
        assertThat(result.user()).isEqualTo(created);
        assertThat(result.roles()).containsExactly(UserRole.USER);
    }

    @Test
    void keepsExistingRolesForExistingUser() {
        ProvisionUserCommand command = new ProvisionUserCommand("keycloak-subject", "new@example.com", "새 이름");
        UserAccount existing = user("old@example.com", "옛 이름");
        UserAccount updated = user("new@example.com", "새 이름");
        when(userAccountRepository.findByAuthSubject("keycloak-subject"))
                .thenReturn(Optional.of(existing), Optional.of(updated));
        when(userRoleRepository.findRoleCodes(10L)).thenReturn(Set.of(UserRole.ADMIN));

        AuthenticatedUser result = service.provision(command);

        verify(userAccountRepository, never()).insertIfAbsent("keycloak-subject", "new@example.com", "새 이름");
        verify(userRoleRepository, never()).assignDefaultRole(10L);
        verify(userAccountRepository).updateProfileIfChanged(10L, "new@example.com", "새 이름");
        assertThat(result.roles()).containsExactly(UserRole.ADMIN);
    }

    private UserAccount user(String email, String name) {
        return new UserAccount(10L, "keycloak-subject", email, name, UserAccount.Status.ACTIVE);
    }
}
