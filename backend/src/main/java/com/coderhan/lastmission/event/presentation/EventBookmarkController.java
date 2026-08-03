package com.coderhan.lastmission.event.presentation;

import java.time.LocalDate;
import java.util.List;
import com.coderhan.lastmission.event.application.EventBookmarkService;
import com.coderhan.lastmission.event.domain.EventPhase;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/api/v1/users/me/bookmarks")
    ApiResponse<List<BookmarkedEventResponse>> getMyBookmarks(@AuthenticationPrincipal LastMissionPrincipal principal) {
        List<BookmarkedEventResponse> events = eventBookmarkService.getMyBookmarkedEvents(principal.userId())
                .stream()
                .map(BookmarkedEventResponse::from)
                .toList();
        return ApiResponse.success(events);
    }

    record BookmarkToggleResponse(boolean isBookmarked) {}

    record BookmarkedEventResponse(
            String id, String title, String categoryName, String venueName,
            LocalDate startDate, LocalDate endDate, EventPhase phase, String thumbnailUrl
    ) {
        static BookmarkedEventResponse from(EventBookmarkService.BookmarkedEvent item) {
            return new BookmarkedEventResponse(
                    Long.toString(item.id()),
                    item.title(),
                    item.categoryName(),
                    item.venueName(),
                    item.startDate(),
                    item.endDate(),
                    item.phase(),
                    item.thumbnailUrl());
        }
    }
}