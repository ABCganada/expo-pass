package com.coderhan.lastmission.event.presentation;

import java.util.List;
import com.coderhan.lastmission.event.application.EventCategoryService;
import com.coderhan.lastmission.event.domain.EventCategory;
import com.coderhan.lastmission.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class EventCategoryController {
    private final EventCategoryService eventCategoryService;

    @GetMapping("/events/categories")
    ApiResponse<List<CategoryResponse>> getCategories() {
        List<CategoryResponse> categories = eventCategoryService.getCategories()
                .stream()
                .map(CategoryResponse::from)
                .toList();
        return ApiResponse.success(categories);
    }

    record CategoryResponse(String id, String code, String name) {
        static CategoryResponse from(EventCategory category) {
            return new CategoryResponse(Long.toString(category.getId()), category.getCode(), category.getName());
        }
    }
}