package com.coderhan.lastmission.event.presentation.response;

import java.time.Instant;
import com.coderhan.lastmission.event.domain.Ticket;

public record TicketResponse(
        String id, String eventId, String name, int price, int quantityTotal, int quantityRemaining,
        int maxPurchasePerUser, Instant saleStartAt, Instant saleEndAt, Instant createdAt, Instant deletedAt
) {
    public static TicketResponse from(Ticket ticket) {
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