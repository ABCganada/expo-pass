package com.coderhan.lastmission.event.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.coderhan.lastmission.event.domain.EventCategory;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventCategoryServiceTest {
    private static final long CATEGORY_ID = 1L;

    @Mock EventCategoryRepository eventCategoryRepository;

    @InjectMocks EventCategoryService service;

    @Test
    void getCategories_활성_카테고리만_반환() {
        EventCategory category = category(true);
        when(eventCategoryRepository.findAllByActiveTrue()).thenReturn(List.of(category));

        List<EventCategory> categories = service.getCategories();

        assertThat(categories).containsExactly(category);
    }

    @Test
    void getAllCategories_비활성_포함_전체_반환() {
        EventCategory active = category(true);
        EventCategory inactive = category(false);
        when(eventCategoryRepository.findAll()).thenReturn(List.of(active, inactive));

        List<EventCategory> categories = service.getAllCategories();

        assertThat(categories).containsExactly(active, inactive);
    }

    @Test
    void toggleActive_존재하면_상태_반전() {
        EventCategory category = category(true);
        when(eventCategoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

        EventCategory result = service.toggleActive(CATEGORY_ID);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    void toggleActive_존재하지_않으면_예외() {
        when(eventCategoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.toggleActive(CATEGORY_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.EVENT_CATEGORY_NOT_FOUND));
    }

    private EventCategory category(boolean active) {
        EventCategory category = new EventCategory("MUSIC", "음악", active);
        ReflectionTestUtils.setField(category, "id", CATEGORY_ID);
        return category;
    }
}