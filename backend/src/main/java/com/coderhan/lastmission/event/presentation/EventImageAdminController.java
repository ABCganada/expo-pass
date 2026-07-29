package com.coderhan.lastmission.event.presentation;

import java.time.Instant;
import java.util.Locale;

import com.coderhan.lastmission.event.application.EventImageService;
import com.coderhan.lastmission.event.domain.EventImage;
import com.coderhan.lastmission.event.domain.EventImageType;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/events/{eventId}/images")
@RequiredArgsConstructor
class EventImageAdminController {
    private final EventImageService eventImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponse<EventImageResponse>> uploadImage(@PathVariable long eventId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("imageType") String imageType,
            @AuthenticationPrincipal LastMissionPrincipal principal, Authentication authentication) {

        EventImage image = eventImageService.uploadImage(
                eventId, principal.userId(), isAdmin(authentication), toImageType(imageType), file);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(EventImageResponse.from(image)));
    }

    private static EventImageType toImageType(String value) {
        try {
            return EventImageType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.EVENT_IMAGE_INVALID_REQUEST,
                    "imageType 값이 올바르지 않습니다. (THUMBNAIL, GENERAL 중 하나여야 합니다.)");
        }
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    record EventImageResponse(
            String id, String eventId, String imageUrl, EventImageType imageType, int displayOrder, Instant createdAt
    ) {
        static EventImageResponse from(EventImage image) {
            return new EventImageResponse(
                    Long.toString(image.getId()),
                    Long.toString(image.getEvent().getId()),
                    image.getImageUrl(),
                    image.getImageType(),
                    image.getDisplayOrder(),
                    image.getCreatedAt());
        }
    }
}