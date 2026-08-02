package com.coderhan.lastmission.event.presentation;

import java.time.Instant;
import java.util.List;
import com.coderhan.lastmission.event.application.TicketService;
import com.coderhan.lastmission.event.application.command.CreateTicketCommand;
import com.coderhan.lastmission.event.application.command.UpdateTicketCommand;
import com.coderhan.lastmission.event.domain.Ticket;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/manager/events/{eventId}/tickets")
@RequiredArgsConstructor
class TicketManagerController {
    private final TicketService ticketService;

    @PostMapping
    ResponseEntity<ApiResponse<TicketResponse>> createTicket(@PathVariable long eventId,
            @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal, Authentication authentication) {
        Ticket ticket = ticketService.createTicket(
                eventId, principal.userId(), isAdmin(authentication), request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(TicketResponse.from(ticket)));
    }

    @PutMapping("/{ticketId}")
    ResponseEntity<ApiResponse<TicketResponse>> updateTicket(@PathVariable long eventId, @PathVariable long ticketId,
            @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal, Authentication authentication) {
        Ticket ticket = ticketService.updateTicket(
                eventId, ticketId, principal.userId(), isAdmin(authentication), request.toCommand());
        return ResponseEntity.ok(ApiResponse.success(TicketResponse.from(ticket)));
    }

    @DeleteMapping("/{ticketId}")
    ResponseEntity<ApiResponse<Void>> deleteTicket(@PathVariable long eventId, @PathVariable long ticketId,
            @AuthenticationPrincipal LastMissionPrincipal principal, Authentication authentication) {
        ticketService.deleteTicket(eventId, ticketId, principal.userId(), isAdmin(authentication));
        return ResponseEntity.ok(ApiResponse.success("티켓을 삭제했습니다.", null));
    }

    @GetMapping
    ApiResponse<List<TicketResponse>> getTickets(@PathVariable long eventId,
            @AuthenticationPrincipal LastMissionPrincipal principal, Authentication authentication) {
        List<TicketResponse> tickets = ticketService.getAdminTickets(eventId, principal.userId(), isAdmin(authentication))
                .stream()
                .map(TicketResponse::from)
                .toList();
        return ApiResponse.success(tickets);
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    record CreateTicketRequest(
            String name, Integer price, Integer quantityTotal, Integer maxPurchasePerUser,
            Instant saleStartAt, Instant saleEndAt
    ) {
        CreateTicketCommand toCommand() {
            return new CreateTicketCommand(name, price, quantityTotal, maxPurchasePerUser, saleStartAt, saleEndAt);
        }
    }

    record UpdateTicketRequest(
            String name, Integer price, Integer quantityTotal, Integer maxPurchasePerUser,
            Instant saleStartAt, Instant saleEndAt
    ) {
        UpdateTicketCommand toCommand() {
            return new UpdateTicketCommand(name, price, quantityTotal, maxPurchasePerUser, saleStartAt, saleEndAt);
        }
    }

    record TicketResponse(
            String id, String eventId, String name, int price, int quantityTotal, int quantityRemaining,
            int maxPurchasePerUser, Instant saleStartAt, Instant saleEndAt, Instant createdAt, Instant deletedAt
    ) {
        static TicketResponse from(Ticket ticket) {
            return new TicketResponse(
                    Long.toString(ticket.getId()),
                    Long.toString(ticket.getEvent().getId()),
                    ticket.getName(),
                    ticket.getPrice(),
                    ticket.getQuantityTotal(),
                    ticket.getQuantityRemaining(),
                    ticket.getMaxPurchasePerUser(),
                    ticket.getSaleStartAt(),
                    ticket.getSaleEndAt(),
                    ticket.getCreatedAt(),
                    ticket.getDeletedAt());
        }
    }
}