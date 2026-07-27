package com.coderhan.lastmission.shared.realtime;

/**
 * 구독 주소별 접근 검사.
 *
 * <p>구독은 클라이언트가 주소 문자열만 보내면 되므로, 막지 않으면 아무 방이나 골라
 * 들여다볼 수 있다. 주소의 의미는 각 모듈이 알고 있으므로 검사도 모듈이 구현한다.</p>
 *
 * <p>어느 구현도 자기 소관이라고 하지 않으면({@link #supports} 가 모두 false)
 * 그 주소는 거절된다 — 새 주소를 추가할 때 검사를 빠뜨리지 않게 하기 위해서다.</p>
 */
public interface SubscriptionGuard {
    /** 이 주소가 자기 소관인지. */
    boolean supports(String destination);

    /** 이 사용자가 이 주소를 구독해도 되는지. 방별 권한은 각 구현이 저장소에서 확인한다. */
    boolean canSubscribe(StompUser user, String destination);
}
