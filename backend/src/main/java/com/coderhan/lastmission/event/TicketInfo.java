package com.coderhan.lastmission.event;

import java.time.Instant;

public record TicketInfo(long ticketId, String name, int price, int maxPurchasePerUser, Instant saleStartAt, Instant saleEndAt) {
}