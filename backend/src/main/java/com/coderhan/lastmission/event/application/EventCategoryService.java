package com.coderhan.lastmission.event.application;

import java.util.List;
import com.coderhan.lastmission.event.domain.EventCategory;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventCategoryService {
    private final EventCategoryRepository eventCategoryRepository;

    @Transactional(readOnly = true)
    public List<EventCategory> getCategories() {
        return eventCategoryRepository.findAllByActiveTrue();
    }

    /**
     * 관리자용 전체 카테고리 목록 조회 (비활성 포함) - SUPER_ADMIN 전용
     */
    @Transactional(readOnly = true)
    public List<EventCategory> getAllCategories() {
        return eventCategoryRepository.findAll();
    }

    /**
     * 카테고리 활성화 토글 - SUPER_ADMIN 전용
     */
    @Transactional
    public EventCategory toggleActive(long categoryId) {
        EventCategory category = eventCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_CATEGORY_NOT_FOUND, "카테고리를 찾을 수 없습니다."));
        category.toggleActive();
        return category;
    }
}