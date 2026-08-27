package com.coderhan.lastmission.shared.realtime;

import java.util.Optional;

/**
 * 핸드셰이크 시점의 로그인 사용자를 알려준다.
 *
 * <p>구현은 인증을 아는 모듈(user)에 있다. 공통 모듈이 특정 도메인 모듈을 알면
 * 의존 방향이 뒤집히고, 나중에 그 모듈이 공통을 쓰기 시작하는 순간 순환이 된다.</p>
 */
public interface RealtimeUserResolver {
    Optional<StompUser> currentUser();
}
