package com.coderhan.lastmission.event.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateEventCommand(
        String title, 
        long categoryId, 
        String hostName, 
        String venueName,
        String address, 
        String detailAddress, 
        String kakaoPlaceId, 
        String legalDongCode,
        BigDecimal latitude, 
        BigDecimal longitude, 
        LocalDate startDate, 
        LocalDate endDate
) {}