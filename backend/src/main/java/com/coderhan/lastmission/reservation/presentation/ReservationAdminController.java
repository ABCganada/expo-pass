package com.coderhan.lastmission.reservation.presentation;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.coderhan.lastmission.reservation.application.ReservationService;
import com.coderhan.lastmission.reservation.domain.OrderStatus;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 전체 관리자(ADMIN) 전용 예약 조회 API.
 *
 * <p>경로가 {@code /api/v1/admin/**} 라 SecurityConfig 에서 ROLE_ADMIN만 접근 가능하다.
 * {@code ReservationManagerController}와 달리 소유권 검증 없이 전체 행사를 대상으로 한다 —
 * PII가 섞인 예약자 명단·체크인은 여기 두지 않고, 집계 수치인 예약 현황만 노출한다.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/reservations")
@RequiredArgsConstructor
class ReservationAdminController {
    private final ReservationService reservationService;

    /** 행사별 예약 현황(상태별 건수) — 담당자 무관 전체 조회. */
    @GetMapping("/events/{eventId}/summary")
    ApiResponse<EventSummaryResponse> getEventSummary(@PathVariable String eventId) {
        ReservationService.EventReservationSummary summary =
                reservationService.getEventSummaryForAdmin(parseEventId(eventId));
        return ApiResponse.success(EventSummaryResponse.from(summary));
    }

    /** 날짜별 예약 건수(예약 추이 그래프용) — 담당자 무관 전체 조회. */
    @GetMapping("/events/{eventId}/daily-counts")
    ApiResponse<List<DailyCountResponse>> getDailyCounts(@PathVariable String eventId) {
        List<DailyCountResponse> counts = reservationService
                .getDailyReservationCountsForAdmin(parseEventId(eventId)).stream()
                .map(DailyCountResponse::from)
                .toList();
        return ApiResponse.success(counts);
    }

    private long parseEventId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.RESERVATION_INVALID_REQUEST, "행사 ID가 올바르지 않습니다.");
        }
    }

    record EventSummaryResponse(String eventId, long totalOrders, Map<OrderStatus, Long> countsByStatus) {
        static EventSummaryResponse from(ReservationService.EventReservationSummary summary) {
            return new EventSummaryResponse(Long.toString(summary.eventId()), summary.totalOrders(),
                    summary.countsByStatus());
        }
    }

    record DailyCountResponse(LocalDate date, long count) {
        static DailyCountResponse from(ReservationService.DailyReservationCount dailyCount) {
            return new DailyCountResponse(dailyCount.date(), dailyCount.count());
        }
    }
}
