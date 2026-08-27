package com.coderhan.lastmission.chat.infrastructure.websocket;

import com.coderhan.lastmission.shared.realtime.StompUser;
import com.coderhan.lastmission.shared.realtime.SubscriptionGuard;
import org.springframework.stereotype.Component;

/**
 * 개인정보가 없는 공개방 디렉터리 변경 신호의 구독을 허용한다.
 *
 * <p>사용자별 {@code /user/queue/chat.changed}는 공용 인바운드 가드가 본인 세션으로
 * 격리하므로 여기서 별도로 처리하지 않는다.</p>
 */
@Component
class ChatDirectorySubscriptionGuard implements SubscriptionGuard {

    @Override
    public boolean supports(String destination) {
        return StompChatDirectoryNotifier.PUBLIC_DESTINATION.equals(destination);
    }

    @Override
    public boolean canSubscribe(StompUser user, String destination) {
        return true;
    }
}
