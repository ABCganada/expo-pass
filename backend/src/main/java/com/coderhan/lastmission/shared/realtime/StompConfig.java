package com.coderhan.lastmission.shared.realtime;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * 공용 STOMP 엔드포인트.
 *
 * <p>연결은 {@code /ws} 하나뿐이고, 갈래는 구독 주소로 나눈다:
 * {@code /topic/chat.{roomId}}, {@code /topic/notice}, {@code /user/queue/notification} 등.</p>
 *
 * <p>구독 세션을 이 파드의 메모리에 들고 있으므로 <strong>replica 는 1이어야 한다.</strong>
 * 늘리려면 {@code enableStompBrokerRelay} 로 외부 브로커를 붙여야 하고, 그 전에는
 * 파드마다 구독 장부가 따로 놀아 메시지가 조용히 유실된다.</p>
 */
@Configuration
@EnableWebSocketMessageBroker
class StompConfig implements WebSocketMessageBrokerConfigurer {
    /** 프록시(nginx ingress, Cloudflare)의 유휴 타임아웃보다 짧아야 연결이 안 끊긴다. */
    private static final long HEARTBEAT_MILLIS = 25_000L;

    private final String[] allowedOrigins;
    private final List<SubscriptionGuard> guards;
    private final RealtimeUserResolver userResolver;

    StompConfig(List<SubscriptionGuard> guards, RealtimeUserResolver userResolver,
            @Value("${lastmission.cors.allowed-origins:"
                    + "https://lastmission.example.com,"
                    + "https://localhost.example.com:3000,"
                    + "http://localhost:3000}") String[] allowedOrigins) {
        this.guards = guards;
        this.userResolver = userResolver;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                // 핸드셰이크는 CSRF 검사를 받지 않으므로 허용 Origin 을 반드시 명시한다(CSWSH).
                .setAllowedOrigins(allowedOrigins)
                .setHandshakeHandler(new PrincipalHandshakeHandler(userResolver));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("stomp-heartbeat-");
        scheduler.initialize();

        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[] { HEARTBEAT_MILLIS, HEARTBEAT_MILLIS })
                .setTaskScheduler(scheduler);
        registry.setUserDestinationPrefix("/user");
        // 클라이언트가 서버로 보내는 경로. 지금은 @MessageMapping 이 하나도 없어서
        // /app 으로 보내면 갈 곳이 없다 — 보내기는 전부 REST 다.
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompInboundGuard(guards));
    }

    /** 핸드셰이크 시점에 사용자를 확정해 세션에 붙인다. */
    private static final class PrincipalHandshakeHandler extends DefaultHandshakeHandler {
        private final RealtimeUserResolver userResolver;

        private PrincipalHandshakeHandler(RealtimeUserResolver userResolver) {
            this.userResolver = userResolver;
        }

        @Override
        protected Principal determineUser(ServerHttpRequest request, WebSocketHandler handler,
                Map<String, Object> attributes) {
            return userResolver.currentUser().orElse(null);
        }
    }
}
