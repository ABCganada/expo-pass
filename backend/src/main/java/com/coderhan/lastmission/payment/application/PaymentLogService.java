package com.coderhan.lastmission.payment.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 결제 로그(PG 연동 요청/응답, 웹훅 수신 감사 로그) 조회 유스케이스. 관리자 전용. */
@Service
@RequiredArgsConstructor
public class PaymentLogService {

    private final PaymentLogRepository paymentLogRepository;

    public PaymentLogPage list(int page, int size) {
        return paymentLogRepository.findAll(page, size);
    }
}
