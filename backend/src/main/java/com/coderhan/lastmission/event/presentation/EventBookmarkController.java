package com.coderhan.lastmission.event.presentation;

import com.coderhan.lastmission.event.application.EventBookmarkService;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class EventBookmarkController {
    private final EventBookmarkService eventBookmarkService;

    @PostMapping("/api/v1/events/{eventId}/bookmark")
    ApiResponse<BookmarkToggleResponse> toggleBookmark(@PathVariable long eventId,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        boolean isBookmarked = eventBookmarkService.toggleBookmark(eventId, principal.userId());
        return ApiResponse.success(new BookmarkToggleResponse(isBookmarked));
    }

    record BookmarkToggleResponse(boolean isBookmarked) {}
}