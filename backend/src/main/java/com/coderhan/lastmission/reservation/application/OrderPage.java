package com.coderhan.lastmission.reservation.application;

import java.util.List;
import com.coderhan.lastmission.reservation.domain.ReservationOrder;

/** 주문 목록 한 페이지(리포지토리 계층 — 아직 티켓 수량이 채워지지 않은 원본 주문). */
public record OrderPage(List<ReservationOrder> orders, int page, int size, long totalElements) {
}
