package com.coderhan.lastmission.event.application;

import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.event.domain.EventCategory;

public interface EventCategoryRepository {
    Optional<EventCategory> findById(Long id);

    // 카테고리 목록 조회용
    List<EventCategory> findAllByActiveTrue();
}