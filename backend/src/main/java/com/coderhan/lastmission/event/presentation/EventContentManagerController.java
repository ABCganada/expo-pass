package com.coderhan.lastmission.event.presentation;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.coderhan.lastmission.event.application.EventContentService;
import com.coderhan.lastmission.event.domain.EventContent;
import com.coderhan.lastmission.event.domain.EventContentType;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/manager/events/{eventId}/contents")
@RequiredArgsConstructor
class EventContentManagerController {
    private final EventContentService eventContentService;

    @PutMapping
    ApiResponse<List<EventContentResponse>> upsertContents(@PathVariable long eventId,
            @RequestBody UpsertEventContentsRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal, Authentication authentication) {
        List<EventContent> contents = eventContentService.upsertContents(
                eventId, principal.userId(), isAdmin(authentication), request.toContentsMap());
        return ApiResponse.success(contents.stream().map(EventContentResponse::from).toList());
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private static EventContentType toContentType(String value) {
        try {
            return EventContentType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST,
                    "contentType 값이 올바르지 않습니다. (DESCRIPTION, NOTICE, LOCATION_GUIDE 중 하나여야 합니다.)");
        }
    }

    record UpsertEventContentsRequest(Map<String, String> contents) {
        Map<EventContentType, String> toContentsMap() {
            if (contents == null) {
                return null;
            }
            return contents.entrySet()
                    .stream()
                    .collect(Collectors.toMap(entry ->
                            toContentType(entry.getKey()), Map.Entry::getValue));
        }
    }

    record EventContentResponse(
            String id, String eventId, EventContentType contentType, String content, Instant updatedAt
    ) {
        static EventContentResponse from(EventContent content) {
            return new EventContentResponse(
                    Long.toString(content.getId()),
                    Long.toString(content.getEvent().getId()),
                    content.getContentType(),
                    content.getContent(),
                    content.getUpdatedAt());
        }
    }
}