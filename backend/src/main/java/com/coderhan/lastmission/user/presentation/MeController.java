package com.coderhan.lastmission.user.presentation;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {
    /**
     * id 는 문자열로 내보낸다. users.id 가 2^53 을 넘어 JSON 숫자로 주면
     * 브라우저에서 값이 깨진다(프로젝트 공통 규약).
     */
    public record MeResponse(String id, String authUuid, String email, String name, Set<String> roles) {}

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal LastMissionPrincipal principal, Authentication authentication) {
        Set<String> roles = new LinkedHashSet<>(authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .toList());
        return new MeResponse(Long.toString(principal.userId()), principal.authSubject(),
                principal.email(), principal.name(), roles);
    }
}
