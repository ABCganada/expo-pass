package com.coderhan.lastmission.reservation.presentation;

import com.coderhan.lastmission.reservation.application.WaitingRoomService;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/reservations/waiting-room")
@RequiredArgsConstructor
class WaitingRoomController {
    private final WaitingRoomService waitingRoomService;

    @GetMapping("/{eventId}/stream")
    SseEmitter enter(@AuthenticationPrincipal LastMissionPrincipal principal, @PathVariable long eventId) {
        return waitingRoomService.enter(principal.userId(), eventId);
    }
}
