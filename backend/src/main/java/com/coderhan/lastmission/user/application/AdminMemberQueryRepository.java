package com.coderhan.lastmission.user.application;

import java.util.Optional;

/**
 * 관리자 회원 관리 전용 조회 창구.
 *
 * <p>다른 모듈에 공개하는 {@link com.coderhan.lastmission.user.UserDirectory} 와 달리
 * user 모듈 내부(관리자 기능)에서만 쓰는 읽기 리포지토리다. 공개 포트를 관리자용
 * 필드(상태·권한)로 오염시키지 않으려고 분리한다.</p>
 */
public interface AdminMemberQueryRepository {
    /** 이름·이메일 부분 검색으로 회원을 페이지 조회한다. 상태 무관(비활성 포함). */
    AdminMemberPage findMembers(String query, int page, int size);

    Optional<AdminMember> findById(long userId);
}
