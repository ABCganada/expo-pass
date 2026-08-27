package com.coderhan.lastmission.user.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import com.coderhan.lastmission.user.application.AuthenticatedUser;
import com.coderhan.lastmission.user.application.ProvisionUserCommand;
import com.coderhan.lastmission.user.application.UserProvisioningService;
import com.coderhan.lastmission.user.UserRole;
import com.coderhan.lastmission.user.domain.UserAccount;
import com.coderhan.auth.client.AuthCheckResponse;
import com.coderhan.auth.client.AuthClient;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.client.RestClientException;

class LastMissionAuthenticationFilterTest {
    private AuthClient authClient;
    private UserProvisioningService service;
    private LastMissionAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        authClient = org.mockito.Mockito.mock(AuthClient.class);
        service = org.mockito.Mockito.mock(UserProvisioningService.class);
        chain = org.mockito.Mockito.mock(FilterChain.class);
        filter = new LastMissionAuthenticationFilter(authClient, service,
                new RequestAttributeSecurityContextRepository());
        request = new MockHttpServletRequest("GET", "/api/v1/chat/rooms");
        response = new MockHttpServletResponse();
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    /**
     * STOMP 엔드포인트는 하위 경로가 없는 {@code /ws} 다. 접두사 {@code /ws/} 로만 보면
     * 걸러져서 핸드셰이크가 인증을 못 받고 401 이 된다(실제로 겪음).
     */
    @Test
    void authenticatesWebSocketHandshakePath() throws Exception {
        MockHttpServletRequest handshake = new MockHttpServletRequest("GET", "/ws");
        MockHttpServletResponse handshakeResponse = new MockHttpServletResponse();
        when(authClient.check(handshake, handshakeResponse)).thenReturn(AuthCheckResponse.unauthenticated());

        filter.doFilter(handshake, handshakeResponse, chain);

        // 필터를 탔다면 미인증이므로 401 이어야 한다. 건너뛰었다면 그대로 통과해 200 이 된다.
        assertThat(handshakeResponse.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(handshake, handshakeResponse);
    }

    @Test
    void skipsPathsOutsideApiAndWebSocket() throws Exception {
        MockHttpServletRequest other = new MockHttpServletRequest("GET", "/auth/csrf");
        MockHttpServletResponse otherResponse = new MockHttpServletResponse();

        filter.doFilter(other, otherResponse, chain);

        verify(chain).doFilter(other, otherResponse);
    }

    @Test
    void rejectsUnauthenticatedRequest() throws Exception {
        when(authClient.check(request, response)).thenReturn(AuthCheckResponse.unauthenticated());
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void createsSpringSecurityAuthenticationFromDatabaseRoles() throws Exception {
        AuthCheckResponse auth = authenticatedResponse();
        UserAccount user = new UserAccount(10L, "keycloak-subject", "user@example.com", "사용자", UserAccount.Status.ACTIVE);
        when(authClient.check(request, response)).thenReturn(auth);
        when(service.provision(new ProvisionUserCommand("keycloak-subject", "user@example.com", "사용자")))
                .thenReturn(new AuthenticatedUser(user, Set.of(UserRole.USER)));
        filter.doFilter(request, response, chain);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getPrincipal()).isEqualTo(new LastMissionPrincipal(10L, "keycloak-subject", "user@example.com", "사용자"));
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        verify(chain).doFilter(request, response);
    }

    @Test
    void rejectsInactiveUser() throws Exception {
        AuthCheckResponse auth = authenticatedResponse();
        UserAccount user = new UserAccount(10L, "keycloak-subject", "user@example.com", "사용자", UserAccount.Status.INACTIVE);
        when(authClient.check(request, response)).thenReturn(auth);
        when(service.provision(new ProvisionUserCommand("keycloak-subject", "user@example.com", "사용자")))
                .thenReturn(new AuthenticatedUser(user, Set.of(UserRole.USER)));
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void reportsAuthCoreFailureAsServiceUnavailable() throws Exception {
        when(authClient.check(request, response)).thenThrow(new RestClientException("unavailable"));
        filter.doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(503);
        verify(chain, never()).doFilter(request, response);
    }

    private AuthCheckResponse authenticatedResponse() {
        return new AuthCheckResponse(true, "사용자", "user@example.com", "keycloak-subject", List.of(), Map.of());
    }
}
