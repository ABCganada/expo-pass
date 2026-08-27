package com.coderhan.lastmission.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * user 모듈이 다른 모듈에 공개하는 조회 창구.
 *
 * <p>예: 채팅 초대는 이메일로 사람을 찾아야 하는데, 그렇다고 chat 이 users 테이블을
 * 직접 읽으면 모듈 경계가 무너진다. 이 인터페이스만 의존하게 한다.</p>
 */
public interface UserDirectory {
    /** 활성 사용자의 전체 권한을 조회한다. 사용자가 없거나 비활성이면 빈 값을 반환한다. */
    Optional<UserAccess> findActiveAccessById(long userId);

    /** 활성 사용자를 이메일로 찾는다. 대소문자·앞뒤 공백은 구현이 정규화한다. */
    Optional<UserRef> findActiveByEmail(String email);

    /** 참가자·접속자 ID에 해당하는 활성 사용자만 반환한다. */
    List<UserRef> findActiveByIds(Collection<Long> userIds);

    /** 관리자용 사용자 선택 목록. 이름 또는 이메일 일부로 검색한다. */
    List<UserRef> searchActive(String query, int limit);
}
