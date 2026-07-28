package com.coderhan.lastmission.user.application;

import java.util.Collection;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.domain.RoleCode;
import com.coderhan.lastmission.user.domain.UserRoles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 회원 권한 관리 유스케이스.
 *
 * <p>권한 집합의 "한 사용자 안" 규칙(USER 상시 보유)은 {@link UserRoles} 값 객체가 담당한다.
 * "시스템에 ADMIN 최소 1명"은 별도 장치 없이 <b>본인의 ADMIN 은 스스로 해제할 수 없다</b>는
 * 규칙으로 보장한다 — 이 화면은 ADMIN 만 들어오므로, 편집자가 자기 ADMIN 을 못 떼면
 * 항상 최소 한 명(=편집자)은 ADMIN 으로 남는다.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminMemberService {
    private final AdminMemberQueryRepository queryRepository;
    private final UserRoleRepository userRoleRepository;

    public AdminMemberPage list(String query, int page, int size) {
        return queryRepository.findMembers(query, page, size);
    }

    @Transactional
    public AdminMember updateRoles(long targetUserId, Collection<RoleCode> requested, long actorUserId) {
        AdminMember target = queryRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_MEMBER_NOT_FOUND, "회원을 찾을 수 없습니다."));

        UserRoles roles = UserRoles.of(requested);
        if (targetUserId == actorUserId && !roles.has(RoleCode.ADMIN)) {
            throw new BusinessException(ErrorCode.ADMIN_SELF_DEMOTION,
                    "본인의 ADMIN 권한은 해제할 수 없습니다.");
        }

        userRoleRepository.replaceRoles(targetUserId, roles.codes());
        return new AdminMember(target.id(), target.email(), target.name(), target.status(), roles.codes());
    }
}
