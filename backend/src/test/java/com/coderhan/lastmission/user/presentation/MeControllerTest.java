package com.coderhan.lastmission.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class MeControllerTest {
    private final MeController controller = new MeController();

    @Test
    void returnsLastMissionUserAndDatabaseRoles() {
        LastMissionPrincipal principal = new LastMissionPrincipal(10L, "keycloak-subject", "user@example.com", "사용자");
        var authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER")));
        MeController.MeResponse response = controller.me(principal, authentication);
        assertThat(response.id()).isEqualTo("10");
        assertThat(response.authUuid()).isEqualTo("keycloak-subject");
        assertThat(response.roles()).containsExactly("ADMIN", "USER");
    }
}
