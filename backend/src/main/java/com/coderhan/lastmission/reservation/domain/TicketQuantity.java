package com.coderhan.lastmission.reservation.domain;

/** 한 주문 안에서 특정 티켓 종류를 몇 장 샀는지(목록 화면 집계용). */
public record TicketQuantity(long ticketId, int quantity) {}
