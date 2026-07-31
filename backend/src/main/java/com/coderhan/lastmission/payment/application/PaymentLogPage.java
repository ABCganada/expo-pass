package com.coderhan.lastmission.payment.application;

import java.util.List;
import com.coderhan.lastmission.payment.domain.PaymentLog;

/** 결제 로그 목록 한 페이지. */
public record PaymentLogPage(List<PaymentLog> logs, int page, int size, long totalElements) {
}
