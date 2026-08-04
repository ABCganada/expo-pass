package com.coderhan.lastmission.payment.application;

import com.coderhan.lastmission.payment.domain.PaymentLog;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 결제 로그(PG 연동 요청/응답, 웹훅 수신 감사 로그) 조회 유스케이스. 관리자 전용. */
@Service
@RequiredArgsConstructor
public class PaymentLogService {

    private final PaymentLogRepository paymentLogRepository;

    @Transactional(readOnly = true)
    public PaymentLogPage list(int page, int size) {
        return paymentLogRepository.findAll(page, size);
    }

    @Transactional(readOnly = true)
    public PaymentLog get(long id) {
        return paymentLogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_LOG_NOT_FOUND, "결제 로그를 찾을 수 없습니다."));
    }
}
