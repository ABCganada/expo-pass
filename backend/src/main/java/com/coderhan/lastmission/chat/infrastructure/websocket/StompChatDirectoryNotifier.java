package com.coderhan.lastmission.chat.infrastructure.websocket;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import com.coderhan.lastmission.chat.application.ChatDirectoryNotifier;
import com.coderhan.lastmission.shared.realtime.AfterCommitExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 채팅 디렉터리 무효화 신호를 STOMP로 전달한다.
 *
 * <p>공개방 카탈로그 변경은 공용 토픽, 개인별 안 읽음·비공개방 변경은 사용자 큐로 보낸다.</p>
 */
@Component
@RequiredArgsConstructor
class StompChatDirectoryNotifier implements ChatDirectoryNotifier {
    static final String PUBLIC_DESTINATION = "/topic/chat-directory.changed";
    private static final String USER_DESTINATION = "/queue/chat.changed";
    private static final ChangeSignal SIGNAL = new ChangeSignal(true);

    private final SimpMessagingTemplate messagingTemplate;
    private final AfterCommitExecutor afterCommitExecutor;

    @Override
    public void publicDirectoryChanged() {
        afterCommitExecutor.execute(
                () -> messagingTemplate.convertAndSend(PUBLIC_DESTINATION, SIGNAL));
    }

    @Override
    public void usersChanged(Collection<Long> userIds) {
        Set<String> recipients = userIds.stream()
                .filter(userId -> userId != null && userId > 0)
                .map(String::valueOf)
                .collect(Collectors.toUnmodifiableSet());
        if (recipients.isEmpty()) return;
        afterCommitExecutor.execute(() -> recipients.forEach(userId ->
                messagingTemplate.convertAndSendToUser(userId, USER_DESTINATION, SIGNAL)));
    }

    private record ChangeSignal(boolean changed) {}
}
