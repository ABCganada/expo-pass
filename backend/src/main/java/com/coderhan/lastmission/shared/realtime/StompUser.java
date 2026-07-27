package com.coderhan.lastmission.shared.realtime;

import java.security.Principal;

/**
 * STOMP 세션에 붙는 사용자 식별자.
 *
 * <p>{@code /user/queue/...} 로 특정 사용자에게만 보내려면 Spring 이 세션마다
 * {@link Principal} 을 알아야 한다. 이름은 사용자 ID 문자열을 쓴다.</p>
 */
public record StompUser(long userId) implements Principal {
    @Override
    public String getName() {
        return Long.toString(userId);
    }
}
