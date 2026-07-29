package com.coderhan.lastmission.reservation.presentation;

import java.time.OffsetDateTime;
import com.coderhan.lastmission.reservation.application.ReservationService;
import com.coderhan.lastmission.reservation.domain.ReservationOrderItem;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 예약 체크인 API.
 *
 * <p>경로가 {@code /api/v1/admin/**} 라 SecurityConfig 에서 ROLE_ADMIN 만 접근 가능하다
 * (AdminMemberController와 동일한 컨벤션 — 컨트롤러엔 별도 권한 체크 코드 없음).</p>
 */
@RestController
@RequestMapping("/api/v1/admin/reservations")
@RequiredArgsConstructor
class ReservationAdminController {
    private final ReservationService reservationService;

    @PostMapping("/checkin")
    ApiResponse<CheckinResponse> checkin(@RequestBody CheckinRequest request,
                                         @AuthenticationPrincipal LastMissionPrincipal principal) {
        ReservationOrderItem item = reservationService.checkin(principal.userId(), request.qrCodeHash());
        return ApiResponse.success(CheckinResponse.from(item));
    }

    record CheckinRequest(String qrCodeHash) {}

    record CheckinResponse(String orderItemId, String orderId, String ticketId, OffsetDateTime checkedInAt) {
        static CheckinResponse from(ReservationOrderItem item) {
            return new CheckinResponse(Long.toString(item.orderItemId()), item.orderId(),
                    Long.toString(item.ticketId()), item.checkedInAt());
        }
    }
}