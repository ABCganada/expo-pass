package com.coderhan.lastmission.chat.infrastructure.websocket;

import java.time.OffsetDateTime;
import com.coderhan.lastmission.chat.application.ChatBroadcaster;
import com.coderhan.lastmission.chat.domain.ChatMessage;
import com.coderhan.lastmission.chat.domain.ChatMessageType;
import com.coderhan.lastmission.shared.realtime.AfterCommitExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 저장된 메시지를 그 방 구독자에게 밀어준다.
 *
 * <p>전달은 브로커가 한다. 여기서는 주소만 정하고 누가 받는지는 모른다 —
 * 나중에 외부 브로커로 바꿔도 이 코드는 그대로다.</p>
 */
@Component
@RequiredArgsConstructor
class ChatStompBroadcaster implements ChatBroadcaster {
    static final String ROOM_DESTINATION_PREFIX = "/topic/chat.";

    private final SimpMessagingTemplate messagingTemplate;
    private final AfterCommitExecutor afterCommitExecutor;

    @Override
    public void broadcast(ChatMessage message) {
        MessagePayload payload = MessagePayload.from(message);
        afterCommitExecutor.execute(() -> messagingTemplate.convertAndSend(
                ROOM_DESTINATION_PREFIX + message.roomId(), payload));
    }

    /** REST 응답과 같은 모양으로 내보낸다. 프론트가 한 가지 형태만 다루면 되도록. */
    record MessagePayload(String id, String roomId, String senderId, String senderName,
            String content, ChatMessageType messageType, boolean deleted, OffsetDateTime createdAt) {
        static MessagePayload from(ChatMessage message) {
            return new MessagePayload(Long.toString(message.id()), Long.toString(message.roomId()),
                    Long.toString(message.senderId()), message.senderName(), message.displayContent(),
                    message.messageType(), message.isDeleted(), message.createdAt());
        }
    }
}
