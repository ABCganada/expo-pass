package com.coderhan.lastmission.user.infrastructure.security;

import java.util.Optional;
import com.coderhan.lastmission.shared.realtime.RealtimeUserResolver;
import com.coderhan.lastmission.shared.realtime.StompUser;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * WebSocket 핸드셰이크에서 사용자를 확정한다.
 *
 * <p>핸드셰이크는 평범한 HTTP GET 이라 서블릿 필터 체인을 타고, 그 시점에는
 * {@code LastMissionAuthenticationFilter} 가 채워 놓은 SecurityContext 가 살아 있다.
 * 연결이 맺어진 뒤의 STOMP 프레임은 HTTP 가 아니므로 여기서 확정한 값을
 * Spring 이 세션에 실어 이후 메시지까지 옮겨 준다.</p>
 */
@Component
class SecurityContextRealtimeUserResolver implements RealtimeUserResolver {
    @Override
    public Optional<StompUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof LastMissionPrincipal principal
                ? Optional.of(new StompUser(principal.userId()))
                : Optional.empty();
    }
}
