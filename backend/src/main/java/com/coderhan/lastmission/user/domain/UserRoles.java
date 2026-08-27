package com.coderhan.lastmission.user.domain;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import com.coderhan.lastmission.user.UserRole;

/**
 * 한 사용자가 가진 권한 집합.
 *
 * <p>불변식: 모든 사용자는 USER 권한을 항상 보유한다(제거 불가, 권한의 바닥값).
 * 요청 집합이 비어 있거나 USER 를 빼려 해도 이 값 객체가 강제로 USER 를 포함시킨다.
 * "한 사용자 안"에서 지켜야 하는 규칙이므로 여기서 못박는다.</p>
 */
public final class UserRoles {
    private final Set<UserRole> codes;

    private UserRoles(Set<UserRole> codes) {
        this.codes = codes;
    }

    /** 요청 권한을 정규화한다. USER 는 무조건 포함된다. */
    public static UserRoles of(Collection<UserRole> requested) {
        EnumSet<UserRole> normalized = EnumSet.of(UserRole.USER);
        if (requested != null) {
            for (UserRole code : requested) {
                if (code != null) normalized.add(code);
            }
        }
        return new UserRoles(normalized);
    }

    public boolean has(UserRole code) {
        return codes.contains(code);
    }

    /** 방어적 복사본을 돌려준다. */
    public Set<UserRole> codes() {
        return EnumSet.copyOf(codes);
    }
}
