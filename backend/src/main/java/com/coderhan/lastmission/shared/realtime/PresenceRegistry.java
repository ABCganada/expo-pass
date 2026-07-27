package com.coderhan.lastmission.shared.realtime;

import java.security.Principal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * 현재 파드에 연결된 STOMP 세션을 사용자 단위로 집계한다.
 *
 * <p>같은 사용자가 여러 탭을 열 수 있으므로 세션 하나가 끊겨도 다른 세션이 남아 있으면
 * 온라인이다. 현재 simple broker와 마찬가지로 단일 파드 메모리 기준이다.</p>
 */
@Component
public class PresenceRegistry {
    static final String CHANGED_DESTINATION = "/topic/presence.changed";

    private final ConcurrentMap<String, SessionPresence> sessions = new ConcurrentHashMap<>();
    private final Clock clock;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceRegistry(Clock clock, SimpMessagingTemplate messagingTemplate) {
        this.clock = clock;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void connected(SessionConnectedEvent event) {
        String sessionId = SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders());
        Principal principal = event.getUser();
        if (sessionId != null && principal instanceof StompUser user) {
            boolean wasOnline = isOnline(user.userId());
            sessions.put(sessionId, new SessionPresence(user.userId(), OffsetDateTime.now(clock)));
            if (!wasOnline) broadcastChanged();
        }
    }

    @EventListener
    public void disconnected(SessionDisconnectEvent event) {
        SessionPresence removed = sessions.remove(event.getSessionId());
        if (removed != null && !isOnline(removed.userId())) broadcastChanged();
    }

    public boolean isOnline(long userId) {
        return sessions.values().stream().anyMatch(session -> session.userId() == userId);
    }

    public Set<Long> onlineUserIds() {
        return sessions.values().stream()
                .map(SessionPresence::userId)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /** 여러 탭 중 가장 먼저 연결된 시각. */
    public Optional<OffsetDateTime> connectedAt(long userId) {
        return sessions.values().stream()
                .filter(session -> session.userId() == userId)
                .map(SessionPresence::connectedAt)
                .min(OffsetDateTime::compareTo);
    }

    private void broadcastChanged() {
        messagingTemplate.convertAndSend(
                CHANGED_DESTINATION, new PresenceChanged(OffsetDateTime.now(clock)));
    }

    /** 사용자 정보 없이 목록이 바뀌었다는 사실만 전달한다. 실제 목록은 관리자 API가 반환한다. */
    private record PresenceChanged(OffsetDateTime changedAt) {}

    private record SessionPresence(long userId, OffsetDateTime connectedAt) {}
}
