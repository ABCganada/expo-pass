package com.coderhan.lastmission.user.application;

import java.util.Set;
import com.coderhan.lastmission.user.domain.RoleCode;
import com.coderhan.lastmission.user.domain.UserAccount;

/**
 * 관리자 회원 관리 화면용 읽기 모델. 계정 정보 + 상태 + 보유 권한을 한데 묶는다.
 */
public record AdminMember(long id, String email, String name, UserAccount.Status status, Set<RoleCode> roles) {
}
