-- ============================================
-- Payment 도메인 테이블 생성 스크립트 (CockroachDB)
-- 사용 PG: 토스페이먼츠 (단일 PG)
-- ============================================

-- 1. payments : 결제
CREATE TABLE payments (
    id INT8 NOT NULL DEFAULT unique_rowid() PRIMARY KEY,
    order_id STRING NOT NULL,               -- Reservation 도메인 reservation_orders.order_id 참조 (String, "ORD-yyyyMMdd-######", 논리적 참조, FK 미설정)
    idempotency_key STRING NOT NULL,        -- 클라이언트가 결제 요청마다 생성해 전달하는 멱등성 키 (재시도 시 동일 결제 결과 반환용)
    amount DECIMAL(12, 2) NOT NULL,
    method STRING NOT NULL,                 -- 'CARD', 'BANK_TRANSFER' 등 결제수단
    status STRING NOT NULL DEFAULT 'REQUESTED'
        CHECK (status IN ('REQUESTED', 'COMPLETED', 'FAILED', 'CANCELLED')),

    pg_provider STRING NOT NULL
        CHECK (pg_provider IN ('TOSS')),    -- 단일 PG지만 추후 확장 대비해 컬럼은 유지
    pg_order_id STRING,                     -- 토스에 전달하는 우리 쪽 주문 식별자 (orderId 규칙이 달라 order_id 그대로 못 쓸 수 있음)
    pg_transaction_id STRING,               -- 토스 최종 거래 식별자 (paymentKey)

    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    INDEX idx_payments_order_id (order_id),
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

-- 2. payment_refunds : 환불 (부분 환불 미허용 → 전액 환불만 지원)
CREATE TABLE payment_refunds (
    id INT8 NOT NULL DEFAULT unique_rowid() PRIMARY KEY,
    payment_id INT8 NOT NULL REFERENCES payments(id),
    amount DECIMAL(12, 2) NOT NULL,         -- 정책상 payments.amount와 동일해야 함 (앱 레벨 검증)
    reason STRING,
    status STRING NOT NULL DEFAULT 'REQUESTED'
        CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'COMPLETED')),
    is_auto_approved BOOL NOT NULL DEFAULT false,  -- D-3 이전 신청 자동승인 여부
    approved_by INT8,                       -- Identity 도메인 user_accounts.id (BIGINT) 참조, 논리적 참조
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    refunded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    INDEX idx_payment_refunds_payment_id (payment_id),
    INDEX idx_payment_refunds_status (status)
);

-- ⭐ 부분 환불 미허용 → 한 결제당 유효 환불은 1건만 (거절/취소 제외)
CREATE UNIQUE INDEX idx_payment_refunds_unique_active
    ON payment_refunds (payment_id)
    WHERE status IN ('REQUESTED', 'APPROVED', 'COMPLETED');

-- 3. payment_settlements : 정산 (행사 단위)
CREATE TABLE payment_settlements (
    id INT8 NOT NULL DEFAULT unique_rowid() PRIMARY KEY,
    event_id INT8 NOT NULL,                 -- Event 도메인 events.id 참조 (BIGINT 잠정 가정 — Event 스키마 확정 전 재확인 필요, 논리적 참조)
    total_sales DECIMAL(14, 2) NOT NULL DEFAULT 0,
    commission_rate DECIMAL(5, 2) NOT NULL DEFAULT 5.00,  -- 확정: 기본 수수료율 5%
    commission_amount DECIMAL(14, 2) NOT NULL DEFAULT 0,
    net_amount DECIMAL(14, 2) NOT NULL DEFAULT 0,
    status STRING NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED')),
    settled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    INDEX idx_payment_settlements_event_id (event_id),
    INDEX idx_payment_settlements_status (status)
);

-- 4. payment_logs : PG 연동 요청/응답 감사 로그 (원본 payload 그대로 JSONB 저장)
CREATE TABLE payment_logs (
    id INT8 NOT NULL DEFAULT unique_rowid() PRIMARY KEY,
    payment_id INT8 NOT NULL REFERENCES payments(id),
    action STRING NOT NULL,                 -- 'REQUEST', 'APPROVE', 'CANCEL', 'REFUND' 등
    request_payload JSONB,
    response_payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    INDEX idx_payment_logs_payment_id (payment_id)
);
