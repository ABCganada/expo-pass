package com.coderhan.lastmission.chat.infrastructure.websocket;

import com.coderhan.lastmission.chat.application.ChatRepository;
import com.coderhan.lastmission.shared.realtime.StompUser;
import com.coderhan.lastmission.shared.realtime.SubscriptionGuard;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * {@code /topic/chat.{roomId}} 구독 검사.
 *
 * <p>연결은 하나이고 방은 구독으로 고르므로, 방별 접근 권한을 구독 시점에 확인한다.
 * 공개방이면 전원, 비공개방이면 현재 참가자만 구독할 수 있다.</p>
 */
@Component
@RequiredArgsConstructor
class ChatSubscriptionGuard implements SubscriptionGuard {
    private static final Logger log = LoggerFactory.getLogger(ChatSubscriptionGuard.class);

    private final ChatRepository chatRepository;

    @Override
    public boolean supports(String destination) {
        return destination.startsWith(ChatStompBroadcaster.ROOM_DESTINATION_PREFIX);
    }

    @Override
    public boolean canSubscribe(StompUser user, String destination) {
        Long roomId = roomIdOf(destination);
        if (roomId == null) return false;
        try {
            // 관리자 특례 없이 공개방 또는 현재 참가자인 비공개방만 허용한다.
            return chatRepository.findRoom(roomId)
                    .filter(room -> room.active())
                    .map(room -> room.isPublic()
                            || chatRepository.isCurrentParticipant(room.id(), user.userId()))
                    .orElse(false);
        } catch (DataAccessException exception) {
            // 조회가 안 되면 방이 유효한지 알 수 없다. 모르면 열어 주지 않는다.
            // 어차피 DB 가 죽으면 메시지 저장도 안 되므로 구독만 살려 둘 이유가 없다.
            log.warn("[chat-ws] 방 조회 실패로 구독 거절 room={}", roomId, exception);
            return false;
        }
    }

    private Long roomIdOf(String destination) {
        String raw = destination.substring(ChatStompBroadcaster.ROOM_DESTINATION_PREFIX.length());
        try {
            long parsed = Long.parseLong(raw);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
