package com.coderhan.lastmission.event.application;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.coderhan.lastmission.event.domain.Event;
import com.coderhan.lastmission.event.domain.EventContent;
import com.coderhan.lastmission.event.domain.EventContentType;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventContentService {
    private final EventRepository eventRepository;
    private final EventContentRepository eventContentRepository;

    /**
     * 행사 콘텐츠 UPSERT - ADMIN은 전체, MANAGER는 본인이 담당(manager_id)하는 행사만.
     */
    @Transactional
    public List<EventContent> upsertContents(long eventId, long callerUserId, boolean isAdmin,
            Map<EventContentType, String> contents) {
        Event event = eventRepository.findNotDeletedById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));

        validateEventAccess(event, callerUserId, isAdmin, "콘텐츠를 수정");
        validateContents(contents);

        // 응답 순서를 enum 선언 순서로 고정
        return new EnumMap<>(contents).entrySet()
                .stream()
                .map(entry -> upsertOne(event, entry.getKey(), entry.getValue()))
                .toList();
    }

    private EventContent upsertOne(Event event, EventContentType contentType, String content) {
        return eventContentRepository.findByEventIdAndContentType(event.getId(), contentType)
                .map(existing -> {
                    existing.updateContent(content);
                    return existing;
                })
                .orElseGet(() -> 
                        eventContentRepository.save(new EventContent(event, contentType, content)));
    }

    /** ADMIN은 전체 허용, 아니면 본인이 담당(manager_id)하는 행사인지 확인 */
    private void validateEventAccess(Event event, long callerUserId, boolean isAdmin, String action) {
        if (!isAdmin && !Objects.equals(event.getManagerId(), callerUserId)) {
            throw new BusinessException(ErrorCode.EVENT_ACCESS_DENIED, "본인이 담당하는 행사만 " + action + "할 수 있습니다.");
        }
    }

    private void validateContents(Map<EventContentType, String> contents) {
        if (contents == null || contents.isEmpty()) {
            throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "수정할 콘텐츠는 최소 1개 이상 필요합니다.");
        }
        for (Map.Entry<EventContentType, String> entry : contents.entrySet()) {
            if (entry.getKey() == null) {
                throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "contentType은 필수입니다.");
            }
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                throw new BusinessException(ErrorCode.EVENT_INVALID_REQUEST, "콘텐츠 내용은 비어 있을 수 없습니다.");
            }
        }
    }
}