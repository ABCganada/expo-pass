package com.coderhan.lastmission.event.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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

        int displayOrder = (int) eventImageRepository.countByEventId(eventId);
        return registerAndUpload(event, imageType, displayOrder, file);
    }

    /**
     * 행사 이미지 다중 등록 - ADMIN은 전체, MANAGER는 본인이 담당(manager_id)하는 행사만.
     * 전체 성공 또는 전체 실패로 취급
     */
    @Transactional
    public List<EventImage> uploadAll(long eventId, long callerUserId, boolean isAdmin,
            List<EventImageType> imageTypes, List<MultipartFile> files) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));

        validateEventAccess(event, callerUserId, isAdmin, "이미지를 등록");
        validateImageFiles(imageTypes, files);

        int displayOrder = (int) eventImageRepository.countByEventId(eventId);

        List<EventImage> saved = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            saved.add(registerAndUpload(event, imageTypes.get(i), displayOrder + i, files.get(i)));
        }
        return saved;
    }

    /**
     * URL 발급(reserveUrl) -> DB save -> 롤백 정리 콜백 등록 -> 실제 업로드(uploadTo, 마지막) 순서의 공통 로직.
     */
    private EventImage registerAndUpload(Event event, EventImageType imageType, int displayOrder, MultipartFile file) {
        String imageUrl = eventImageStorage.reserveUrl(event.getId(), file.getOriginalFilename());
        EventImage image = new EventImage(event, imageUrl, imageType, displayOrder);
        EventImage saved = eventImageRepository.save(image);

        // 실제 업로드 시도 전에 등록해야 업로드 도중 실패까지 커버됨
        registerCleanupOnRollback(imageUrl);
        uploadToStorage(imageUrl, file);

        return saved;
    }

    /**
     * 행사 이미지 삭제 - ADMIN은 전체, MANAGER는 본인이 담당(manager_id)하는 행사만.
     */
    @Transactional
    public void deleteImage(long eventId, long imageId, long callerUserId, boolean isAdmin) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        validateEventAccess(event, callerUserId, isAdmin, "이미지를 삭제");

        EventImage image = eventImageRepository.findByIdAndEventId(imageId, eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_IMAGE_NOT_FOUND, "이미지를 찾을 수 없습니다."));

        eventImageRepository.delete(image);
        registerCleanupAfterCommit(image.getImageUrl());
    }

    private void registerCleanupOnRollback(String imageUrl) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                // STATUS_UNKNOWN은 삭제 대상에서 제외
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    eventImageStorage.cleanup(imageUrl);
                }
            }
        });
    }
    
    private void registerCleanupAfterCommit(String imageUrl) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventImageStorage.cleanup(imageUrl);
            }
        });
    }


    private void uploadToStorage(String imageUrl, MultipartFile file) {
        try {
            eventImageStorage.uploadTo(imageUrl, file.getContentType(), file.getBytes());
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

    private void validateImageFiles(List<EventImageType> imageTypes, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ErrorCode.EVENT_IMAGE_INVALID_REQUEST, "이미지 파일은 최소 1개 이상 필요합니다.");
        }
        if (imageTypes == null || imageTypes.size() != files.size()) { // 파일마다 대응되는 타입 정보 반드시 하나씩
            throw new BusinessException(ErrorCode.EVENT_IMAGE_INVALID_REQUEST, "imageType과 파일 개수가 일치하지 않습니다.");
        }
        files.forEach(this::validateImageFile);
    }
}