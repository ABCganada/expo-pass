package com.coderhan.lastmission.shared.realtime;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

/**
 * 클라이언트가 올려보내는 STOMP 프레임을 검사한다.
 *
 * <p>연결이 맺어진 뒤의 프레임은 HTTP 가 아니라 서블릿 필터(= Spring Security)를 타지
 * 않는다. 그래서 여기서 따로 막는다. 사용자는 핸드셰이크에서 확정돼 세션에 실려 오므로
 * {@code SecurityContextHolder} 가 아니라 {@link StompHeaderAccessor#getUser()} 를 본다.</p>
 *
 * <p>막는 것은 둘이다:</p>
 * <ul>
 *   <li><b>SEND</b> — 전부 거절한다. 브로커 주소({@code /topic/...})로 보낸 SEND 는
 *       기본 설정에서 <em>그대로 구독자에게 중계된다.</em> 열어 두면 로그인한 사용자가
 *       REST 의 검증·저장을 건너뛰고 아무 내용이나 채팅방에 뿌릴 수 있다.
 *       보내기는 REST 하나로만 유지한다.</li>
 *   <li><b>SUBSCRIBE</b> — 주소별 권한을 {@link SubscriptionGuard} 에 물어본다.
 *       소관을 주장하는 구현이 없으면 거절한다. 새 주소를 추가하며 검사를 빠뜨리면
 *       열리는 게 아니라 막히므로 실수의 결과가 안전한 쪽으로 기운다.</li>
 * </ul>
 *
 * <p>거절은 조용히 버리지 않고 예외를 던진다. 프레임을 삼키면 클라이언트는 구독이 된 줄
 * 알고 아무것도 못 받는 상태로 남아, 원인을 찾기 어려운 고장이 된다.</p>
 */
class StompInboundGuard implements ChannelInterceptor {
    private static final Logger log = LoggerFactory.getLogger(StompInboundGuard.class);

    private final List<SubscriptionGuard> guards;

    StompInboundGuard(List<SubscriptionGuard> guards) {
        this.guards = guards;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == null) return message;

        return switch (command) {
            case SEND -> throw reject(accessor, "이 연결로는 메시지를 보낼 수 없습니다.");
            case SUBSCRIBE -> requireSubscribable(accessor, message);
            default -> message;
        };
    }

    private Message<?> requireSubscribable(StompHeaderAccessor accessor, Message<?> message) {
        String destination = accessor.getDestination();
        if (destination == null) throw reject(accessor, "구독 주소가 없습니다.");

        if (!(accessor.getUser() instanceof StompUser user)) {
            throw reject(accessor, "인증되지 않은 연결입니다.");
        }

        // /user/queue/... 는 Spring 이 세션별로 격리하므로 본인 것만 받는다.
        if (destination.startsWith("/user/")) return message;

        boolean allowed = guards.stream()
                .filter(guard -> guard.supports(destination))
                .findFirst()
                .map(guard -> guard.canSubscribe(user, destination))
                .orElse(false);

        if (!allowed) {
            log.debug("[stomp] 구독 거절 user={} destination={}", user.userId(), destination);
            throw reject(accessor, "구독할 수 없는 주소입니다.");
        }
        return message;
    }

    private MessagingException reject(StompHeaderAccessor accessor, String reason) {
        log.debug("[stomp] 거절 command={} session={} 사유={}",
                accessor.getCommand(), accessor.getSessionId(), reason);
        return new MessagingException(reason);
    }
}
