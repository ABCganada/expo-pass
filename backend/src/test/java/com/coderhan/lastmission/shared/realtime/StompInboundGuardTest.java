package com.coderhan.lastmission.shared.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

/**
 * 연결이 맺어진 뒤의 STOMP 프레임은 HTTP 가 아니라 Spring Security 를 타지 않는다.
 * 여기서 막는 것이 유일한 방어선이므로 동작을 못 박아 둔다.
 */
class StompInboundGuardTest {
    private static final long USER_ID = 7L;

    /** {@code /topic/room.*} 만 소관으로 삼고, 짝수 방만 허용하는 테스트용 검사기. */
    private static final SubscriptionGuard ROOM_GUARD = new SubscriptionGuard() {
        @Override
        public boolean supports(String destination) {
            return destination.startsWith("/topic/room.");
        }

        @Override
        public boolean canSubscribe(StompUser user, String destination) {
            return destination.endsWith("2");
        }
    };

    private final StompInboundGuard guard = new StompInboundGuard(List.of(ROOM_GUARD));

    /**
     * 브로커 주소로 온 SEND 는 기본 설정에서 그대로 구독자에게 중계된다.
     * 열어 두면 REST 의 검증·저장을 건너뛰고 아무 내용이나 뿌릴 수 있다.
     */
    @Test
    void rejectsSendSoThatPublishingStaysOnRestOnly() {
        Message<?> send = frame(StompCommand.SEND, "/topic/room.2", USER_ID);

        assertThatThrownBy(() -> guard.preSend(send, null))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("보낼 수 없습니다");
    }

    @Test
    void allowsSubscriptionThatGuardApproves() {
        Message<?> subscribe = frame(StompCommand.SUBSCRIBE, "/topic/room.2", USER_ID);

        assertThat(guard.preSend(subscribe, null)).isSameAs(subscribe);
    }

    @Test
    void rejectsSubscriptionThatGuardDenies() {
        Message<?> subscribe = frame(StompCommand.SUBSCRIBE, "/topic/room.1", USER_ID);

        assertThatThrownBy(() -> guard.preSend(subscribe, null))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("구독할 수 없는");
    }

    /** 소관을 주장하는 검사기가 없으면 거절한다 — 새 주소를 추가하며 검사를 빠뜨려도 열리지 않도록. */
    @Test
    void rejectsDestinationNoGuardClaims() {
        Message<?> subscribe = frame(StompCommand.SUBSCRIBE, "/topic/notice", USER_ID);

        assertThatThrownBy(() -> guard.preSend(subscribe, null))
                .isInstanceOf(MessagingException.class);
    }

    /** {@code /user/**} 는 Spring 이 세션별로 격리하므로 본인 것만 받는다. */
    @Test
    void allowsPersonalQueueWithoutGuard() {
        Message<?> subscribe = frame(StompCommand.SUBSCRIBE, "/user/queue/notification", USER_ID);

        assertThat(guard.preSend(subscribe, null)).isSameAs(subscribe);
    }

    @Test
    void rejectsSubscriptionWithoutAuthenticatedUser() {
        Message<?> subscribe = frame(StompCommand.SUBSCRIBE, "/topic/room.2", null);

        assertThatThrownBy(() -> guard.preSend(subscribe, null))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("인증되지 않은");
    }

    @Test
    void rejectsSubscriptionWithoutDestination() {
        Message<?> subscribe = frame(StompCommand.SUBSCRIBE, null, USER_ID);

        assertThatThrownBy(() -> guard.preSend(subscribe, null))
                .isInstanceOf(MessagingException.class);
    }

    /** 연결·해지·종료는 통과시킨다. 막으면 정상 연결이 성립하지 않는다. */
    @Test
    void passesThroughLifecycleCommands() {
        for (StompCommand command : List.of(StompCommand.CONNECT, StompCommand.UNSUBSCRIBE, StompCommand.DISCONNECT)) {
            Message<?> message = frame(command, null, USER_ID);
            assertThat(guard.preSend(message, null)).isSameAs(message);
        }
    }

    private static Message<?> frame(StompCommand command, String destination, Long userId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) accessor.setDestination(destination);
        if (userId != null) accessor.setUser(new StompUser(userId));
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
