package com.coderhan.lastmission.payment.presentation;

import java.time.OffsetDateTime;
import java.util.List;
import com.coderhan.lastmission.payment.application.PaymentLogPage;
import com.coderhan.lastmission.payment.application.PaymentLogService;
import com.coderhan.lastmission.payment.domain.PaymentLog;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 결제 로그(PG 연동 요청/응답, 웹훅 수신 감사 로그) 조회 API. 관리자 전용. */
@RestController
@RequestMapping("/api/v1/admin/payments/logs")
@RequiredArgsConstructor
class PaymentLogController {

    private final PaymentLogService paymentLogService;

    /** 결제 로그 목록 조회. 최신순 페이지네이션. */
    @GetMapping
    ApiResponse<PaymentLogPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PaymentLogPage result = paymentLogService.list(page, size);
        return ApiResponse.success(PaymentLogPageResponse.from(result));
    }

    /** 결제 로그 상세 조회. */
    @GetMapping("/{id}")
    ApiResponse<PaymentLogResponse> get(@PathVariable String id) {
        PaymentLog log = paymentLogService.get(parseId(id));
        return ApiResponse.success(PaymentLogResponse.from(log));
    }

    private long parseId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.PAYMENT_LOG_NOT_FOUND, "결제 로그를 찾을 수 없습니다.");
        }
    }

    record PaymentLogResponse(
            String id,
            String paymentKey,
            String action,
            String requestPayload,
            String responsePayload,
            String webhookTransmissionId,
            OffsetDateTime createdAt
    ) {
        static PaymentLogResponse from(PaymentLog log) {
            return new PaymentLogResponse(
                    Long.toString(log.id()),
                    log.paymentKey(),
                    log.action(),
                    log.requestPayload(),
                    log.responsePayload(),
                    log.webhookTransmissionId(),
                    log.createdAt()
            );
        }
    }

    record PaymentLogPageResponse(
            List<PaymentLogResponse> logs,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        static PaymentLogPageResponse from(PaymentLogPage page) {
            List<PaymentLogResponse> logs = page.logs().stream()
                    .map(PaymentLogResponse::from)
                    .toList();

            int totalPages = page.size() == 0
                    ? 0
                    : (int) Math.ceil((double) page.totalElements() / page.size());

            return new PaymentLogPageResponse(logs, page.page(), page.size(), page.totalElements(), totalPages);
        }
    }
}
