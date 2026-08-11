-- =====================================================================
-- V8 : 도안 리뷰·포인트 (요구사항 REVIEW-001~010, POINT-001~006)
-- ERD v1.3 도메인 5. 핵심 불변식은 애플리케이션이 아니라 DB 제약으로 강제한다.
--   · 도안당 리뷰 1개        → uq_review_active (부분 유니크)
--   · 리뷰당 포인트 1회 적립  → uq_point_earn_per_review (부분 유니크)
--   · 포인트 잔액 = 거래합계  → users.point_balance 는 캐시, 진실은 point_transaction
-- =====================================================================

-- ---------------------------------------------------------------------
-- PATTERN_REVIEW : 도안에 속한 독립 게시물. 오늘의 로그에서 '복사'해 독립시킨다.
--   source_post_id·source_project_id 는 FK 아님 — 원본을 지워도 리뷰는 남는다(복사의 의미).
--   gauge_adjustment_summary 도 참조가 아니라 작성 시점 복사(REVIEW-010).
-- ---------------------------------------------------------------------
CREATE TABLE pattern_review (
    id                       BIGSERIAL    PRIMARY KEY,
    user_id                  BIGINT       NOT NULL,
    pattern_id               BIGINT       NOT NULL,
    library_id               BIGINT       NOT NULL,   -- 구매 검증 근거. 환불 후에도 유지
    source_post_id           BIGINT,                  -- FK 아님(불러온 로그 기록용)
    source_project_id        BIGINT,                  -- FK 아님(조정 요약 표시용)
    title                    VARCHAR(200),
    content_document         JSONB,                   -- 로그에서 복사한 본문
    content_text             TEXT,                    -- 검색용 평문
    cover_file_id            BIGINT,
    knitting_status          VARCHAR(20),             -- 작성 시점 상태 복사
    gauge_adjustment_summary VARCHAR(200),            -- 작성 시점 조정 요약 복사(예: 품 +5cm · 소매 -3cm)
    visibility               VARCHAR(20)  NOT NULL DEFAULT 'PUBLIC',
    moderation_status        VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    like_count               INT          NOT NULL DEFAULT 0,
    comment_count            INT          NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at               TIMESTAMPTZ,
    CONSTRAINT ck_review_visibility CHECK (visibility IN ('PUBLIC','PRIVATE')),
    CONSTRAINT ck_review_moderation CHECK (moderation_status IN ('NORMAL','HIDDEN')),
    CONSTRAINT ck_review_status CHECK (knitting_status IS NULL
        OR knitting_status IN ('PLANNED','CO','WIP','UFO','FO')),
    CONSTRAINT fk_review_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_review_pattern FOREIGN KEY (pattern_id) REFERENCES selling_pattern (id),
    CONSTRAINT fk_review_library FOREIGN KEY (library_id) REFERENCES pattern_library (id),
    CONSTRAINT fk_review_cover   FOREIGN KEY (cover_file_id) REFERENCES file_asset (id)
);

-- 도안당 리뷰 1개(REVIEW-003) — 동시 이중 등록도 DB가 막는다.
CREATE UNIQUE INDEX uq_review_active ON pattern_review (user_id, pattern_id) WHERE deleted_at IS NULL;
-- 리뷰 목록(REVIEW-001): 공개·정상·미삭제 필터
CREATE INDEX idx_review_list ON pattern_review (pattern_id, visibility, deleted_at);

-- ---------------------------------------------------------------------
-- REVIEW_IMAGE : 로그 이미지를 복사할 때 파일 실체가 아니라 file_id 만 공유한다.
-- ---------------------------------------------------------------------
CREATE TABLE review_image (
    id         BIGSERIAL PRIMARY KEY,
    review_id  BIGINT    NOT NULL,
    file_id    BIGINT    NOT NULL,
    sort_order INT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_review_image_review FOREIGN KEY (review_id) REFERENCES pattern_review (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_image_file   FOREIGN KEY (file_id)   REFERENCES file_asset (id)
);
CREATE INDEX idx_review_image_review ON review_image (review_id);

-- ---------------------------------------------------------------------
-- POINT_TRANSACTION : 포인트 거래 원장. amount 는 부호 포함(적립 +, 사용 −).
--   balance_after 는 거래 직후 잔액(회계 표준 — 어긋난 지점 추적용).
-- ---------------------------------------------------------------------
CREATE TABLE point_transaction (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    tx_type         VARCHAR(20)  NOT NULL,
    amount          INT          NOT NULL,
    balance_after   INT          NOT NULL,
    reason_code     VARCHAR(30),
    review_id       BIGINT,
    order_id        BIGINT,
    idempotency_key VARCHAR(100) NOT NULL,
    fail_reason     TEXT,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_point_tx_type CHECK (tx_type IN ('EARN','USE','REVOKE','RETURN','EXPIRE')),
    CONSTRAINT ck_point_balance_nonneg CHECK (balance_after >= 0),
    CONSTRAINT uq_point_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_point_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 리뷰당 1회 적립(POINT-001) — 중복 지급 차단
CREATE UNIQUE INDEX uq_point_earn_per_review ON point_transaction (review_id) WHERE tx_type = 'EARN';
-- 포인트 내역(POINT-006)
CREATE INDEX idx_point_user ON point_transaction (user_id, created_at DESC);
