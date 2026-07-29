package com.coderhan.lastmission.event.application.command;

import java.time.Instant;

public record UpdateTicketCommand(
        String name,
        Integer price,
        Integer maxPurchasePerUser,
        Instant saleStartAt,
        Instant saleEndAt
) {}