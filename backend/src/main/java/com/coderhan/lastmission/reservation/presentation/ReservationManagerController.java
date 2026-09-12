package com.coderhan.lastmission.reservation.presentation;

import java.math.BigDecimal;
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
 * 관리자 전용 예약 관리 API — 체크인, 예약자 명단, 행사별 예약 현황.
 *
 * <p>경로가 {@code /api/v1/manager/**} 라 SecurityConfig 에서 ROLE_ADMIN, ROLE_MANAGER 가 접근 가능하다
 * (박람회관리자가 자신이 담당하는 행사의 예약을 관리하기 위한 API — 컨트롤러엔 별도 권한 체크 코드 없음).</p>
 */
@RestController
@RequestMapping("/api/v1/manager/reservations")
@RequiredArgsConstructor
class ReservationManagerController {
    private final ReservationService reservationService;

    @PostMapping("/checkin")
    ApiResponse<CheckinResponse> checkin(@RequestBody CheckinRequest request,
                                         @AuthenticationPrincipal LastMissionPrincipal principal) {
        ReservationOrderItem item = reservationService.checkin(principal.userId(), request.qrCodeHash());
        return ApiResponse.success(CheckinResponse.from(item));
    }

    /** 예약자 명단 조회. 유저 이름/이메일까지 채워서 내려준다. */
    @GetMapping("/events/{eventId}/attendees")
    ApiResponse<List<AttendeeResponse>> getAttendees(@PathVariable String eventId) {
        List<AttendeeResponse> attendees = reservationService.getEventAttendees(parseEventId(eventId)).stream()
                .map(info -> AttendeeResponse.from(info.order(), info.userRef()))
                .toList();
        return ApiResponse.success(attendees);
    }

    /** 행사별 예약 현황(상태별 건수). */
    @GetMapping("/events/{eventId}/summary")
    ApiResponse<EventSummaryResponse> getEventSummary(@PathVariable String eventId) {
        ReservationService.EventReservationSummary summary =
                reservationService.getEventSummary(parseEventId(eventId));
        return ApiResponse.success(EventSummaryResponse.from(summary));
    }

    @GetMapping("/events/{eventId}/checkin-status")
    ApiResponse<CheckinProgressResponse> getCheckinStatus(@PathVariable String eventId){
        CheckinProgress checkinStatus =
                reservationService.getCheckinProgress(parseEventId(eventId));
        return ApiResponse.success(CheckinProgressResponse.from(checkinStatus));
    }

    private long parseEventId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.RESERVATION_INVALID_REQUEST, "행사 ID가 올바르지 않습니다.");
        }
    }

    record CheckinRequest(String qrCodeHash) {}

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

}