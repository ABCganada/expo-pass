package com.coderhan.lastmission.reservation.presentation;

import com.coderhan.lastmission.reservation.application.WaitingRoomService;
import com.coderhan.lastmission.reservation.domain.WaitingTicket;
import com.coderhan.lastmission.reservation.domain.WaitingTicketStatus;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/waiting-room")
@RequiredArgsConstructor
class WaitingRoomController {
    private final WaitingRoomService waitingRoomService;

    @PostMapping("/{eventId}/enter")
    ResponseEntity<ApiResponse<TicketResponse>> enter(@PathVariable String eventId,
                                                      @AuthenticationPrincipal LastMissionPrincipal principal) {
        WaitingTicket ticket = waitingRoomService.enterQueue(principal.userId(), parseEventId(eventId));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(TicketResponse.from(ticket)));
    }

    @GetMapping("/{eventId}/status")
    ApiResponse<StatusResponse> status(@PathVariable String eventId,
                                       @AuthenticationPrincipal LastMissionPrincipal principal) {
        WaitingRoomService.StatusResult result =
                waitingRoomService.getStatus(principal.userId(), parseEventId(eventId));
        return ApiResponse.success(StatusResponse.from(result));
    }

    private long parseEventId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.RESERVATION_INVALID_REQUEST, "행사 ID가 올바르지 않습니다.");
        }
    }

    record TicketResponse(String ticketNo, WaitingTicketStatus status) {
        static TicketResponse from(WaitingTicket ticket) {
            return new TicketResponse(Long.toString(ticket.ticketNo()), ticket.status());
        }
    }

    record StatusResponse(WaitingTicketStatus status, Long position) {
        static StatusResponse from(WaitingRoomService.StatusResult result) {
            return new StatusResponse(result.status(), result.position());
        }
    }
}