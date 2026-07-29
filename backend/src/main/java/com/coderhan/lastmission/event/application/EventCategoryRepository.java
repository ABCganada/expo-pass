package com.coderhan.lastmission.event.application;

import java.util.Optional;
import com.coderhan.lastmission.event.domain.EventCategory;

public interface EventCategoryRepository {
    Optional<EventCategory> findById(Long id);
}