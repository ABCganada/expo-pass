package com.coderhan.lastmission.payment.presentation;

import com.coderhan.lastmission.payment.application.TossWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 토스페이먼츠 웹훅 수신. 인증 없이 열려있는 공개 엔드포인트다(SecurityConfig에서
 * permitAll + CSRF 예외 처리 필요) — 발신 IP 검증은 별도 작업으로 남겨둔다(TODO).
 *
 * 파싱/저장 로직은 {@link TossWebhookService}가 실패해도 예외를 던지지 않도록 보장하므로,
 * 이 컨트롤러는 항상 200을 반환한다(토스 10초 룰).
 */
@RestController
@RequestMapping("/webhooks/payments/toss")
@RequiredArgsConstructor
class TossWebhookController {

    private final TossWebhookService tossWebhookService;

    @PostMapping
    ResponseEntity<Void> receive(
            @RequestHeader(value = "tosspayments-webhook-transmission-id", required = false) String transmissionId,
            @RequestBody String rawBody
    ) {
        tossWebhookService.receive(transmissionId, rawBody);
        return ResponseEntity.ok().build();
    }
}
