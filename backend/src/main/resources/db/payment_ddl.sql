-- ============================================
-- Payment 도메인 테이블 생성 스크립트 (CockroachDB)
-- 사용 PG: 토스페이먼츠 (단일 PG)
-- ============================================

-- 1. payments : 결제
CREATE TABLE payments (
    id INT8 NOT NULL DEFAULT unique_rowid() PRIMARY KEY,
    order_id STRING NOT NULL,               -- Reservation 도메인 reservation_orders.order_id 또는 Marketing 도메인 marketing_banner_ads.order_id 참조 (String, 논리적 참조, FK 미설정)
    order_type STRING NOT NULL
        CHECK (order_type IN ('RESERVATION', 'ADVERTISEMENT')),  -- order_id가 어느 도메인 주문을 가리키는지 (예약/광고)
    user_id INT8,                           -- Identity 도메인 user_accounts.id 참조, 논리적 참조 (내 결제 내역 조회용)
    idempotency_key STRING NOT NULL,        -- 클라이언트가 결제 요청마다 생성해 전달하는 멱등성 키 (재시도 시 동일 결제 결과 반환용)
    amount DECIMAL(12, 2) NOT NULL,
    method STRING NOT NULL,                 -- 'CARD', 'BANK_TRANSFER' 등 결제수단
    status STRING NOT NULL DEFAULT 'REQUESTED'
        CHECK (status IN ('REQUESTED', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED')),

    pg_provider STRING NOT NULL
        CHECK (pg_provider IN ('TOSS')),    -- 단일 PG지만 추후 확장 대비해 컬럼은 유지
    pg_order_id STRING,                     -- 토스에 전달하는 우리 쪽 주문 식별자 (orderId 규칙이 달라 order_id 그대로 못 쓸 수 있음)
    pg_transaction_id STRING,               -- 토스 최종 거래 식별자 (paymentKey)

    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    INDEX idx_payments_order_id (order_id),
    INDEX idx_payments_user_id (user_id),
    INDEX idx_payments_status (status),
    INDEX idx_payments_pg_provider (pg_provider)
);

-- ⭐ 이중 결제 방지: order_id 기준, 취소/실패를 제외한(PENDING/COMPLETED) 행만 유니크 (부분 유니크 인덱스)
-- PENDING 단계에서부터 막아야 동일 주문에 대한 결제 시도 자체가 PG로 중복 전달되는 걸 방지할 수 있음
CREATE UNIQUE INDEX idx_payments_unique_active
    ON payments (order_id)
    WHERE status IN ('PENDING', 'COMPLETED');

-- ⭐ 멱등성 처리: 동일 idempotency_key로는 결제 요청이 한 번만 처리되도록 전역 유니크
CREATE UNIQUE INDEX idx_payments_idempotency_key
    ON payments (idempotency_key);

-- 2. payment_refunds : 환불 (행사 시작까지 남은 일수에 따라 환불율을 자동 적용 — 부분 환불 허용, D-0은 0% 환불)
CREATE TABLE payment_refunds (
    id INT8 NOT NULL DEFAULT unique_rowid() PRIMARY KEY,
    payment_id INT8 NOT NULL REFERENCES payments(id),
    amount DECIMAL(12, 2) NOT NULL,         -- payments.amount × 환불율(정책상 계산, 0 이상 payments.amount 이하)
    reason STRING,
    status STRING NOT NULL DEFAULT 'COMPLETED'
        CHECK (status IN ('COMPLETED')),
    is_auto_approved BOOL NOT NULL DEFAULT true,  -- 정책상 모든 환불이 자동승인되므로 항상 true
    approved_by INT8,                       -- Identity 도메인 user_accounts.id (BIGINT) 참조, 논리적 참조. 수동 승인 플로우 없음 — 항상 null
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    refunded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    INDEX idx_payment_refunds_payment_id (payment_id),
    INDEX idx_payment_refunds_status (status)
);

-- ⭐ 결제 1건당 유효 환불(COMPLETED)은 1건만 — 중복 환불 신청 방지
CREATE UNIQUE INDEX idx_payment_refunds_unique_active
    ON payment_refunds (payment_id)
    WHERE status = 'COMPLETED';

-- 3. payment_settlements : 정산 (행사 단위, 행사 종료 감지 즉시 확정 — 대기 상태 없음)
CREATE TABLE payment_settlements (
    id INT8 NOT NULL DEFAULT unique_rowid() PRIMARY KEY,
    event_id INT8 NOT NULL,                 -- Event 도메인 events.id 참조 (BIGINT 잠정 가정 — Event 스키마 확정 전 재확인 필요, 논리적 참조)
    total_sales DECIMAL(14, 2) NOT NULL DEFAULT 0,
    commission_rate DECIMAL(5, 2) NOT NULL DEFAULT 5.00,  -- 확정: 수수료율 5% 고정 (관리자 설정 API 없음)
    commission_amount DECIMAL(14, 2) NOT NULL DEFAULT 0,
    net_amount DECIMAL(14, 2) NOT NULL DEFAULT 0,
    status STRING NOT NULL DEFAULT 'COMPLETED'
        CHECK (status IN ('COMPLETED')),
    settled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    INDEX idx_payment_settlements_status (status)
);

-- ⭐ 행사 1건당 정산은 1건만 — 행사 종료 이벤트가 중복 발행/처리돼도 정산이 두 번 생기지 않도록 방지
CREATE UNIQUE INDEX idx_payment_settlements_event_id
    ON payment_settlements (event_id);

-- 4. payment_ad_settlements : 광고 정산 (광고 단위, 광고 만료 감지 즉시 확정 — 대기 상태 없음)
CREATE TABLE payment_ad_settlements (
    id INT8 NOT NULL DEFAULT unique_rowid() PRIMARY KEY,
    ad_id UUID NOT NULL,                    -- Marketing 도메인 marketing_banner_ads.id 참조, 논리적 참조 (FK 미설정)
    total_amount DECIMAL(14, 2) NOT NULL DEFAULT 0,
    commission_rate DECIMAL(5, 2) NOT NULL DEFAULT 5.00,  -- 확정: 수수료율 5% 고정 (관리자 설정 API 없음)
    commission_amount DECIMAL(14, 2) NOT NULL DEFAULT 0,
    net_amount DECIMAL(14, 2) NOT NULL DEFAULT 0,
    status STRING NOT NULL DEFAULT 'COMPLETED'
        CHECK (status IN ('COMPLETED')),
    settled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    INDEX idx_payment_ad_settlements_status (status)
);

-- ⭐ 광고 1건당 정산은 1건만 — 광고 만료 이벤트가 중복 발행/처리돼도 정산이 두 번 생기지 않도록 방지
CREATE UNIQUE INDEX idx_payment_ad_settlements_ad_id
    ON payment_ad_settlements (ad_id);

-- 5. payment_logs : PG 연동 요청/응답 + 토스 웹훅 수신 감사 로그 (원본 payload 그대로 JSONB 저장)
CREATE TABLE payment_logs (
    id INT8 NOT NULL DEFAULT unique_rowid() PRIMARY KEY,
    payment_key STRING,                     -- 토스 paymentKey 그대로 저장 (FK 없음, 논리적 참조 — payments.pg_transaction_id와 조회 시점에만 조인)
    action STRING NOT NULL,                 -- 'REQUEST'/'APPROVE'/'CANCEL'/'REFUND'(우리 쪽 호출) 또는 'PAYMENT_STATUS_CHANGED'/'CANCEL_STATUS_CHANGED'(토스 웹훅) 등
    webhook_transmission_id STRING,         -- 토스 웹훅 tosspayments-webhook-transmission-id 헤더값. 우리 쪽 API 호출 로그는 null
    request_payload JSONB,
    response_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    INDEX idx_payment_logs_payment_key (payment_key)
);

-- ⭐ 웹훅 재시도로 같은 이벤트가 여러 번 와도 중복 저장되지 않도록 방지 (우리 쪽 API 호출 로그는 webhook_transmission_id가 null이라 대상 아님)
CREATE UNIQUE INDEX idx_payment_logs_webhook_transmission_id
    ON payment_logs (webhook_transmission_id)
    WHERE webhook_transmission_id IS NOT NULL;
