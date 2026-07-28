package com.coderhan.lastmission.user.presentation;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import com.coderhan.lastmission.user.application.AdminMember;
import com.coderhan.lastmission.user.application.AdminMemberPage;
import com.coderhan.lastmission.user.application.AdminMemberService;
import com.coderhan.lastmission.user.domain.RoleCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 회원 권한 관리 API.
 *
 * <p>ID 는 문자열로 내보낸다. CockroachDB 의 id 가 2^53 을 넘어 JS Number 로는 정확히
 * 표현되지 않기 때문이다(프로젝트 공통 규약).</p>
 *
 * <p>경로가 {@code /api/v1/admin/**} 라 SecurityConfig 에서 ROLE_ADMIN 만 접근 가능하다.</p>
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
class AdminMemberController {
    private final AdminMemberService adminMemberService;

    @GetMapping("/members")
    ApiResponse<MemberPageResponse> members(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AdminMemberPage result = adminMemberService.list(query, page, size);
        return ApiResponse.success(MemberPageResponse.from(result));
    }

    @PutMapping("/members/{id}/roles")
    ApiResponse<MemberResponse> updateRoles(@PathVariable String id,
            @RequestBody UpdateRolesRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        AdminMember updated = adminMemberService.updateRoles(parseId(id), parseRoles(request), principal.userId());
        return ApiResponse.success("권한을 변경했습니다.", MemberResponse.from(updated));
    }

    private long parseId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.ADMIN_MEMBER_NOT_FOUND, "회원을 찾을 수 없습니다.");
        }
    }

    private Set<RoleCode> parseRoles(UpdateRolesRequest request) {
        List<String> raw = request == null || request.roles() == null ? List.of() : request.roles();
        EnumSet<RoleCode> parsed = EnumSet.noneOf(RoleCode.class);
        for (String value : raw) {
            if (value == null || value.isBlank()) continue;
            try {
                parsed.add(RoleCode.from(value));
            } catch (IllegalArgumentException exception) {
                throw new BusinessException(ErrorCode.ADMIN_ROLE_INVALID, "알 수 없는 권한입니다: " + value);
            }
        }
        return parsed;
    }

    record UpdateRolesRequest(List<String> roles) {}

    record MemberResponse(String id, String email, String name, String status, List<String> roles) {
        static MemberResponse from(AdminMember member) {
            List<String> roles = member.roles().stream().map(Enum::name).sorted().toList();
            return new MemberResponse(Long.toString(member.id()), member.email(), member.name(),
                    member.status().name(), roles);
        }
    }

    record MemberPageResponse(List<MemberResponse> members, int page, int size,
            long totalElements, int totalPages) {
        static MemberPageResponse from(AdminMemberPage page) {
            List<MemberResponse> members = page.members().stream()
                    .map(MemberResponse::from).toList();
            int totalPages = page.size() == 0
                    ? 0
                    : (int) Math.ceil((double) page.totalElements() / page.size());
            return new MemberPageResponse(members, page.page(), page.size(), page.totalElements(), totalPages);
        }
    }
}
