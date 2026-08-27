package com.coderhan.lastmission.reservation.presentation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import com.coderhan.lastmission.reservation.application.ReservationService;
import com.coderhan.lastmission.reservation.domain.CheckinProgress;
import com.coderhan.lastmission.reservation.domain.OrderStatus;
import com.coderhan.lastmission.reservation.domain.ReservationOrder;
import com.coderhan.lastmission.reservation.domain.ReservationOrderItem;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import com.coderhan.lastmission.user.UserRef;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 매니저 전용 예약 관리 API — 체크인, 예약자 명단, 행사별 예약 현황.
 *
 * <p>경로가 {@code /api/v1/manager/**} 라 SecurityConfig 에서 ROLE_ADMIN, ROLE_MANAGER 가 접근 가능하지만,
 * 여기서는 호출자의 role과 무관하게 항상 본인이 담당하는 행사만 접근할 수 있도록
 * {@code ReservationService}가 매 호출마다 소유권을 검증한다(ADMIN도 예외 없음 — EventManagerController와
 * 동일한 방식). 소유권 검증 없이 전체 행사를 보려면 {@code ReservationAdminController}(/api/v1/admin/reservations)를 쓴다.</p>
 */
@RestController
@RequestMapping("/api/v1/manager/reservations")
@RequiredArgsConstructor
class ReservationManagerController {
    private final ReservationService reservationService;

    @PostMapping("/checkin")
    ApiResponse<CheckinResponse> checkin(@RequestBody CheckinRequest request,
                                         @AuthenticationPrincipal LastMissionPrincipal principal) {
        ReservationOrderItem item = reservationService.checkin(
                principal.userId(), request.qrCodeHash(), parseEventId(request.eventId()));
        return ApiResponse.success(CheckinResponse.from(item));
    }

    /** 예약자 명단 조회. 유저 이름/이메일까지 채워서 내려준다. */
    @GetMapping("/events/{eventId}/attendees")
    ApiResponse<List<AttendeeResponse>> getAttendees(@PathVariable String eventId,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        List<AttendeeResponse> attendees = reservationService
                .getEventAttendees(parseEventId(eventId), principal.userId()).stream()
                .map(info -> AttendeeResponse.from(info.order(), info.userRef()))
                .toList();
        return ApiResponse.success(attendees);
    }

    /** 행사별 예약 현황(상태별 건수). */
    @GetMapping("/events/{eventId}/summary")
    ApiResponse<EventSummaryResponse> getEventSummary(@PathVariable String eventId,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        ReservationService.EventReservationSummary summary = reservationService.getEventSummary(
                parseEventId(eventId), principal.userId());
        return ApiResponse.success(EventSummaryResponse.from(summary));
    }

    @GetMapping("/events/{eventId}/checkin-status")
    ApiResponse<CheckinProgressResponse> getCheckinStatus(@PathVariable String eventId,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        CheckinProgress checkinStatus = reservationService.getCheckinProgress(
                parseEventId(eventId), principal.userId());
        return ApiResponse.success(CheckinProgressResponse.from(checkinStatus));
    }

    /** 날짜별 예약 건수(예약 추이 그래프용). */
    @GetMapping("/events/{eventId}/daily-counts")
    ApiResponse<List<DailyCountResponse>> getDailyCounts(@PathVariable String eventId,
            @AuthenticationPrincipal LastMissionPrincipal principal) {
        List<DailyCountResponse> counts = reservationService
                .getDailyReservationCounts(parseEventId(eventId), principal.userId()).stream()
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

    record CheckinRequest(String qrCodeHash, String eventId) {}

    record CheckinResponse(String orderItemId, String orderId, String ticketId, OffsetDateTime checkedInAt) {
        static CheckinResponse from(ReservationOrderItem item) {
            return new CheckinResponse(Long.toString(item.orderItemId()), item.orderId(),
                    Long.toString(item.ticketId()), item.checkedInAt());
        }
    }

    record AttendeeResponse(
            String orderId, String userId, String userName, String userEmail,
            OrderStatus status, BigDecimal totalAmount, OffsetDateTime reservedAt
    ) {
        // userRef가 null이면(탈퇴 등으로 활성 유저가 아니면) 이름/이메일은 비워서 내려준다.
        static AttendeeResponse from(ReservationOrder order, UserRef userRef) {
            return new AttendeeResponse(order.orderId(), Long.toString(order.userId()),
                    userRef == null ? null : userRef.name(), userRef == null ? null : userRef.email(),
                    order.status(), order.totalAmount(), order.reservedAt());
        }
    }

    record EventSummaryResponse(String eventId, long totalOrders, Map<OrderStatus, Long> countsByStatus) {
        static EventSummaryResponse from(ReservationService.EventReservationSummary summary) {
            return new EventSummaryResponse(Long.toString(summary.eventId()), summary.totalOrders(),
                    summary.countsByStatus());
        }
    }
    record CheckinProgressResponse(long totalItems, long checkedInCount) {
        static CheckinProgressResponse from(CheckinProgress progress) {
            return new CheckinProgressResponse(progress.totalItems(), progress.checkedInCount());
        }
    }

    record DailyCountResponse(LocalDate date, long count) {
        static DailyCountResponse from(ReservationService.DailyReservationCount dailyCount) {
            return new DailyCountResponse(dailyCount.date(), dailyCount.count());
        }
    }

}