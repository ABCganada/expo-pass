package com.coderhan.lastmission.user;

/**
 * 사용자 단위 직렬화가 필요한 작업에서 쓰는 잠금.
 *
 * <p>users 테이블은 이 모듈의 소유이므로, 다른 모듈이 직접 잠그지 않고 이 인터페이스를 통한다.</p>
 */
public interface UserAccountLock {
    /** 현재 트랜잭션이 끝날 때까지 해당 사용자 행을 잠근다. */
    void lockForUpdate(long userId);
}
