-- 광고 슬롯 (관리자가 생성)
CREATE TABLE marketing_banner_slots (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    max_count   INT NOT NULL DEFAULT 1,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 광고 (마케터가 등록)
CREATE TABLE marketing_banner_ads (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slot_id     UUID NOT NULL REFERENCES marketing_banner_slots(id),
    title       VARCHAR(200) NOT NULL,
    image_url   VARCHAR(500) NOT NULL,
    link_url    VARCHAR(500),
    priority    INT NOT NULL DEFAULT 0,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    starts_at   TIMESTAMPTZ NOT NULL,
    ends_at     TIMESTAMPTZ NOT NULL,
    created_by  VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
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
