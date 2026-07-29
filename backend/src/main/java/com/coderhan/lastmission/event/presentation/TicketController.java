package com.coderhan.lastmission.event.presentation;

import java.time.Instant;
import java.util.List;
import com.coderhan.lastmission.event.application.TicketService;
import com.coderhan.lastmission.event.domain.Ticket;
import com.coderhan.lastmission.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
class TicketController {
    private final TicketService ticketService;

    @GetMapping("/{eventId}/tickets")
    ApiResponse<List<TicketResponse>> getTickets(@PathVariable long eventId) {
        List<TicketResponse> tickets = ticketService.getPublicTickets(eventId)
                .stream()
                .map(TicketResponse::from)
                .toList();
        return ApiResponse.success(tickets);
    }

    record TicketResponse(
            String id, String name, int price, int quantityRemaining,
            int maxPurchasePerUser, Instant saleStartAt, Instant saleEndAt
    ) {
        static TicketResponse from(Ticket ticket) {
            return new TicketResponse(
                    Long.toString(ticket.getId()),
                    ticket.getName(),
                    ticket.getPrice(),
                    ticket.getQuantityRemaining(),
                    ticket.getMaxPurchasePerUser(),
                    ticket.getSaleStartAt(),
                    ticket.getSaleEndAt());
        }
    }
}