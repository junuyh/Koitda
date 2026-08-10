-- =====================================================================
-- V2 : 도안 카탈로그 (요구사항 PATTERN-001~006·009·011)
-- ERD v1.3 도메인 2. 카탈로그 조회와 위시에 필요한 테이블만 담는다.
-- 판매 파일 버전·변경이력 등 '판매 관리' 테이블은 판매자 슬라이스에서 추가한다.
-- =====================================================================

-- 통합 검색(도안명·원작자)을 ILIKE 부분일치로 빠르게 하기 위한 트라이그램 확장
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------------------------------------------------------------------
-- PATTERN_CATEGORY : 자기참조 계층. recommended_measurement_keys 로 등록 화면 기본 실측 행 제시
-- ---------------------------------------------------------------------
CREATE TABLE pattern_category (
    id            BIGSERIAL   PRIMARY KEY,
    name          VARCHAR(50) NOT NULL,
    parent_id     BIGINT,
    sort_order    INT         NOT NULL DEFAULT 0,
    is_active     BOOLEAN     NOT NULL DEFAULT true,
    recommended_measurement_keys JSONB,
    CONSTRAINT fk_category_parent
        FOREIGN KEY (parent_id) REFERENCES pattern_category (id)
);

-- ---------------------------------------------------------------------
-- SELLER_PROFILE : 도안의 판매 주체. user_id 와 1:1
--   platform_fee_rate 는 정산에 쓰이는 스냅샷 기준값(초기 10%)
-- ---------------------------------------------------------------------
CREATE TABLE seller_profile (
    id                BIGSERIAL     PRIMARY KEY,
    user_id           BIGINT        NOT NULL,
    brand_name        VARCHAR(100)  NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    platform_fee_rate NUMERIC(5,2)  NOT NULL DEFAULT 10.00,
    approved_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_seller_profile_user UNIQUE (user_id),
    CONSTRAINT ck_seller_profile_status CHECK (status IN ('ACTIVE','SUSPENDED')),
    CONSTRAINT fk_seller_profile_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

-- ---------------------------------------------------------------------
-- SELLING_PATTERN : 도안 본체
--   금액은 정수 원(BIGINT). 게이지·사이즈는 확정 JSON Schema(gauge_info/size_info)
--   wish_count 등은 목록 표시용 비정규화 카운터(진실의 출처는 각 원본 테이블)
-- ---------------------------------------------------------------------
CREATE TABLE selling_pattern (
    id                   BIGSERIAL    PRIMARY KEY,
    seller_id            BIGINT       NOT NULL,
    title                VARCHAR(200) NOT NULL,
    designer_name        VARCHAR(100),
    category_id          BIGINT,
    craft_type           VARCHAR(20)  NOT NULL,
    difficulty           VARCHAR(20),
    language             VARCHAR(10),
    regular_price        BIGINT,
    sale_price           BIGINT,
    product_form         VARCHAR(30),
    delivery_method      VARCHAR(30),
    availability_days    INT,
    reference_video_url  VARCHAR(500),
    page_count           INT,
    yarn_requirement     TEXT,
    needle_info          JSONB,
    technique_info       JSONB,
    gauge_info           JSONB,
    size_info            JSONB,
    measurement_info     JSONB,   -- size_info 로 통합됨. 하위 호환용 유지
    description          TEXT,
    current_file_id      BIGINT,
    product_status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    wish_count           INT          NOT NULL DEFAULT 0,
    public_project_count INT          NOT NULL DEFAULT 0,
    review_count         INT          NOT NULL DEFAULT 0,
    view_count           INT          NOT NULL DEFAULT 0,
    published_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at           TIMESTAMPTZ,
    CONSTRAINT ck_pattern_craft_type CHECK (craft_type IN ('CROCHET','KNIT')),
    CONSTRAINT ck_pattern_product_status
        CHECK (product_status IN ('DRAFT','PENDING','APPROVED','REJECTED','SUSPENDED')),
    CONSTRAINT ck_pattern_price_nonneg
        CHECK ((regular_price IS NULL OR regular_price >= 0)
           AND (sale_price   IS NULL OR sale_price   >= 0)),
    CONSTRAINT fk_pattern_seller
        FOREIGN KEY (seller_id) REFERENCES seller_profile (id),
    CONSTRAINT fk_pattern_category
        FOREIGN KEY (category_id) REFERENCES pattern_category (id),
    CONSTRAINT fk_pattern_current_file
        FOREIGN KEY (current_file_id) REFERENCES file_asset (id)
);

-- ---------------------------------------------------------------------
-- SELLING_PATTERN_IMAGE : 도안 이미지 (실체는 객체 저장소, 여기선 file_asset 참조)
-- ---------------------------------------------------------------------
CREATE TABLE selling_pattern_image (
    id           BIGSERIAL PRIMARY KEY,
    pattern_id   BIGINT    NOT NULL,
    file_id      BIGINT    NOT NULL,
    sort_order   INT       NOT NULL DEFAULT 0,
    is_thumbnail BOOLEAN   NOT NULL DEFAULT false,
    CONSTRAINT fk_pattern_image_pattern
        FOREIGN KEY (pattern_id) REFERENCES selling_pattern (id) ON DELETE CASCADE,
    CONSTRAINT fk_pattern_image_file
        FOREIGN KEY (file_id) REFERENCES file_asset (id)
);

-- ---------------------------------------------------------------------
-- WISH : 위시(PATTERN-009). 복합 PK 가 '계정당 도안 1회'를 DB 레벨에서 강제한다.
-- ---------------------------------------------------------------------
CREATE TABLE wish (
    user_id    BIGINT      NOT NULL,
    pattern_id BIGINT      NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_wish PRIMARY KEY (user_id, pattern_id),
    CONSTRAINT fk_wish_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_wish_pattern
        FOREIGN KEY (pattern_id) REFERENCES selling_pattern (id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- 인덱스 (ERD 11)
-- ---------------------------------------------------------------------
CREATE INDEX idx_pattern_list      ON selling_pattern (product_status, published_at DESC);
CREATE INDEX idx_pattern_filter    ON selling_pattern (category_id, craft_type, difficulty);
CREATE INDEX idx_pattern_search    ON selling_pattern
    USING gin (title gin_trgm_ops, designer_name gin_trgm_ops);  -- PATTERN-001 통합 검색
CREATE INDEX idx_pattern_image_pat ON selling_pattern_image (pattern_id);
CREATE INDEX idx_wish_pattern      ON wish (pattern_id);  -- 도안별 위시 집계용(복합 PK 는 user 선두라 별도 필요)
