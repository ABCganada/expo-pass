package com.coderhan.lastmission.event.application;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventImage;
import com.coderhan.lastmission.event.domain.EventImageType;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class EventImageService {
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final EventRepository eventRepository;
    private final EventImageRepository eventImageRepository;
    private final EventImageStorage eventImageStorage;

    /**
     * 행사 이미지 등록 - ADMIN은 전체, MANAGER는 본인이 담당(manager_id)하는 행사만.
     */
    @Transactional
    public EventImage uploadImage(long eventId, long callerUserId, boolean isAdmin,
            EventImageType imageType, MultipartFile file) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));

        validateEventAccess(event, callerUserId, isAdmin, "이미지를 등록");
        validateImageFile(file);

        String imageUrl = uploadToStorage(eventId, file);
        int displayOrder = (int) eventImageRepository.countByEventId(eventId);
        EventImage image = new EventImage(event, imageUrl, imageType, displayOrder);

        return eventImageRepository.save(image);
    }

    private String uploadToStorage(long eventId, MultipartFile file) {
        try {
            return eventImageStorage.upload(eventId, file.getOriginalFilename(), file.getContentType(), file.getBytes());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.EVENT_IMAGE_INVALID_REQUEST, "이미지 파일을 읽을 수 없습니다.");
        }
    }

    /** ADMIN은 전체 허용, 아니면 본인이 담당(manager_id)하는 행사인지 확인 */
    private void validateEventAccess(Event event, long callerUserId, boolean isAdmin, String action) {
        if (!isAdmin && !Objects.equals(event.getManagerId(), callerUserId)) {
            throw new BusinessException(ErrorCode.EVENT_ACCESS_DENIED, "본인이 담당하는 행사만 " + action + "할 수 있습니다.");
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.EVENT_IMAGE_INVALID_REQUEST, "이미지 파일은 필수입니다.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.EVENT_IMAGE_INVALID_REQUEST,
                    "이미지 파일 형식은 JPEG, PNG, GIF, WEBP만 허용됩니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.EVENT_IMAGE_INVALID_REQUEST, "이미지 파일은 5MB를 초과할 수 없습니다.");
        }
    }
}