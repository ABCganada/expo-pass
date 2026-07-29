package com.coderhan.lastmission.event.application;

import java.util.List;
import com.coderhan.lastmission.event.domain.EventCategory;
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
}