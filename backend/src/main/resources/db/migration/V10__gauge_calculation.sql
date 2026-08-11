-- =====================================================================
-- V10 : 게이지 계산 (요구사항 GAUGE-001~017)
-- 계산은 서버 수식으로만 처리한다(AI 아님, AI-001). 결과·입력·계산식을 함께 저장해 검산 가능(GAUGE-009).
--   · 적용 계산은 니팅로그당 1건    → uq_gauge_applied (부분 유니크, GAUGE-013)
--   · 공개는 니팅로그 공개설정을 따름 → 별도 컬럼 없음(GAUGE-014)
-- =====================================================================

CREATE TABLE gauge_calculation (
    id                        BIGSERIAL    PRIMARY KEY,
    project_id                BIGINT       NOT NULL,
    pattern_gauge             JSONB,       -- 계산 시점 도안 게이지 스냅샷
    my_gauge                  JSONB,       -- 계산 시점 내 게이지 스냅샷
    selected_size_label       VARCHAR(50),
    target_measurements       JSONB,       -- 사용자가 변경한 부위 목표값(변경분만)
    result                    JSONB        NOT NULL,  -- 계산 결과 전체 스냅샷
    adjusted_cast_on_stitches INT,
    adjustment_summary        VARCHAR(200),           -- '품 +5cm · 소매 -3cm' (GAUGE-015)
    has_adjustment            BOOLEAN      NOT NULL DEFAULT false,
    is_applied                BOOLEAN      NOT NULL DEFAULT false,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_gauge_calc_project FOREIGN KEY (project_id) REFERENCES knitting_project (id) ON DELETE CASCADE
);

-- 적용 중인 계산은 니팅로그당 1건만(GAUGE-013).
CREATE UNIQUE INDEX uq_gauge_applied ON gauge_calculation (project_id) WHERE is_applied;
-- 계산 이력 조회.
CREATE INDEX idx_gauge_calc_project ON gauge_calculation (project_id, created_at DESC);
