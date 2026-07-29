package com.coderhan.lastmission.event.application.command;

import java.time.Instant;

public record CreateTicketCommand(
        String name,
        Integer price,
        Integer quantityTotal,
        Integer maxPurchasePerUser,
        Instant saleStartAt,
        Instant saleEndAt
) {}