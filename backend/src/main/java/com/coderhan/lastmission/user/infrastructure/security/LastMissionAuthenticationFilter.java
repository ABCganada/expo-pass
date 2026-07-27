package com.coderhan.lastmission.user.infrastructure.security;

import java.io.IOException;
import java.util.List;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import com.coderhan.lastmission.user.application.AuthenticatedUser;
import com.coderhan.lastmission.user.application.ProvisionUserCommand;
import com.coderhan.lastmission.user.application.UserProvisioningService;
import com.coderhan.auth.client.AuthCheckResponse;
import com.coderhan.auth.client.AuthClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.filter.OncePerRequestFilter;

public class LastMissionAuthenticationFilter extends OncePerRequestFilter {
    private final AuthClient authClient;
    private final UserProvisioningService userProvisioningService;

    public LastMissionAuthenticationFilter(AuthClient authClient, UserProvisioningService userProvisioningService) {
        this.authClient = authClient;
        this.userProvisioningService = userProvisioningService;
    }

    /**
     * {@code /api/**} 와 WebSocket 핸드셰이크({@code /ws}, {@code /ws/**})만 인증한다.
     *
     * <p>WebSocket 핸드셰이크는 HTTP GET 한 번이므로 이 필터를 그대로 태울 수 있다.
     * 연결이 맺어진 뒤의 STOMP 프레임은 HTTP 가 아니라 이 필터를 타지 않는다 —
     * 핸드셰이크에서 확정한 사용자를 Spring 이 세션에 실어 이후 메시지까지 옮겨 준다.</p>
     *
     * <p>STOMP 엔드포인트가 하위 경로 없는 {@code /ws} 라서 접두사만 보면 걸러진다.
     * 정확히 일치하는 경우를 따로 봐야 한다.</p>
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) return true;
        String path = requestPath(request);
        return !(path.startsWith("/api/") || path.equals("/ws") || path.startsWith("/ws/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        AuthCheckResponse authResponse;
        try {
            authResponse = authClient.check(request, response);
        } catch (RestClientException exception) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "중앙 인증 서비스를 확인할 수 없습니다.");
            return;
        }
        if (!authResponse.authenticated() || !StringUtils.hasText(authResponse.subject())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다.");
            return;
        }

        AuthenticatedUser authenticatedUser = userProvisioningService.provision(new ProvisionUserCommand(
                authResponse.subject(), authResponse.email(), authResponse.name()));
        if (!authenticatedUser.user().isActive()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "비활성화된 사용자입니다.");
            return;
        }

        List<SimpleGrantedAuthority> authorities = authenticatedUser.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
        LastMissionPrincipal principal = new LastMissionPrincipal(authenticatedUser.user().id(),
                authenticatedUser.user().authSubject(), authenticatedUser.user().email(), authenticatedUser.user().name());
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        filterChain.doFilter(request, response);
    }

    private String requestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return StringUtils.hasLength(contextPath) ? requestUri.substring(contextPath.length()) : requestUri;
    }
}
