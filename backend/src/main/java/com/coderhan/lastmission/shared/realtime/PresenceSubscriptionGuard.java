package com.coderhan.lastmission.shared.realtime;

import org.springframework.stereotype.Component;

/**
 * 접속 현황 변경 신호 구독을 허용한다.
 *
 * <p>신호에는 사용자 ID나 이름이 없고 변경 시각만 들어 있다. 실제 접속자 목록은
 * 관리자 권한을 검사하는 REST API에서만 반환한다.</p>
 */
@Component
class PresenceSubscriptionGuard implements SubscriptionGuard {

    @Override
    public boolean supports(String destination) {
        return PresenceRegistry.CHANGED_DESTINATION.equals(destination);
    }

    @Override
    public boolean canSubscribe(StompUser user, String destination) {
        return true;
    }
}
