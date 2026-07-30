-- 광고 슬롯 (BANNER / TAB 고정, 관리자가 생성·삭제하지 않음)
CREATE TABLE marketing_banner_slots (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    max_count     INT NOT NULL DEFAULT 1,
    type          VARCHAR(20) NOT NULL,          -- BANNER | TAB
    price_per_day BIGINT NOT NULL DEFAULT 0,    -- 일 단위 단가 (totalAmount = price_per_day × days)
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 초기 데이터 (최초 1회 수동 실행)
INSERT INTO marketing_banner_slots (name, max_count, type, price_per_day)
SELECT '메인 배너', 3, 'BANNER', 50000
WHERE NOT EXISTS (SELECT 1 FROM marketing_banner_slots WHERE type = 'BANNER');

INSERT INTO marketing_banner_slots (name, max_count, type, price_per_day)
SELECT '광고 탭', 5, 'TAB', 30000
WHERE NOT EXISTS (SELECT 1 FROM marketing_banner_slots WHERE type = 'TAB');

-- 광고 (마케터가 등록)
CREATE TABLE marketing_banner_ads (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(200) NOT NULL,
    banner_image_url VARCHAR(500),
    ad_image_url    VARCHAR(500),
    link_url        VARCHAR(500),
    priority        INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    starts_at       TIMESTAMPTZ NOT NULL,
    ends_at         TIMESTAMPTZ NOT NULL,
    created_by      VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    total_amount    BIGINT
);

-- 광고-슬롯 N:M 조인 테이블
CREATE TABLE marketing_banner_ad_slots (
    ad_id   UUID NOT NULL REFERENCES marketing_banner_ads(id),
    slot_id UUID NOT NULL REFERENCES marketing_banner_slots(id),
    PRIMARY KEY (ad_id, slot_id)
);

-- 노출 통계 (일별)
CREATE TABLE marketing_banner_impressions (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_id     UUID NOT NULL REFERENCES marketing_banner_ads(id),
    stat_date DATE NOT NULL,
    count     BIGINT NOT NULL DEFAULT 0,
    UNIQUE (ad_id, stat_date)
);

-- 클릭 통계 (일별)
CREATE TABLE marketing_banner_clicks (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_id     UUID NOT NULL REFERENCES marketing_banner_ads(id),
    stat_date DATE NOT NULL,
    count     BIGINT NOT NULL DEFAULT 0,
    UNIQUE (ad_id, stat_date)
);
