-- =====================================================================
-- V3 : 니팅로그·외부도안·재료 (요구사항 PROJECT-001~017)
-- ERD v1.3 도메인 3. 니팅로그 생성/조회와 재료 다중 입력, 원작 정보 스냅샷.
-- =====================================================================

-- ---------------------------------------------------------------------
-- EXTERNAL_PATTERN : 사용자 소유 외부 도안. 유사 이름 자동 병합 금지 → UNIQUE 없음.
--   PDF·도안 본문 이미지를 저장하지 않으므로 file_id 컬럼이 없다.
-- ---------------------------------------------------------------------
CREATE TABLE external_pattern (
    id             BIGSERIAL     PRIMARY KEY,
    user_id        BIGINT        NOT NULL,
    title          VARCHAR(200)  NOT NULL,
    creator_name   VARCHAR(100),
    purchase_place VARCHAR(200),
    purchase_url   VARCHAR(500),
    purchase_price BIGINT,
    purchase_date  DATE,
    memo           TEXT,
    craft_type     VARCHAR(20),
    category_id    BIGINT,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_external_craft_type CHECK (craft_type IS NULL OR craft_type IN ('CROCHET','KNIT')),
    CONSTRAINT fk_external_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_external_category FOREIGN KEY (category_id) REFERENCES pattern_category (id)
);

-- ---------------------------------------------------------------------
-- KNITTING_PROJECT : 니팅로그. 도안 XOR 외부도안. 상태는 최신 로그에서 파생한 캐시.
-- ---------------------------------------------------------------------
CREATE TABLE knitting_project (
    id                  BIGSERIAL    PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    selling_pattern_id  BIGINT,
    external_pattern_id BIGINT,
    title               VARCHAR(200),
    display_title       VARCHAR(200) NOT NULL,
    title_sequence      INT,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
    visibility          VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE',
    public_log_count    INT          NOT NULL DEFAULT 0,
    pattern_snapshot    JSONB,
    pattern_version_no  INT,
    snapshot_at         TIMESTAMPTZ,
    note                TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ,
    purge_at            TIMESTAMPTZ,
    -- 도안 XOR 외부도안 (정확히 하나)
    CONSTRAINT ck_project_pattern_xor CHECK (
        (selling_pattern_id IS NOT NULL AND external_pattern_id IS NULL)
        OR (selling_pattern_id IS NULL AND external_pattern_id IS NOT NULL)),
    CONSTRAINT ck_project_status CHECK (status IN ('PLANNED','CO','WIP','UFO','FO')),
    CONSTRAINT ck_project_visibility CHECK (visibility IN ('PRIVATE','PUBLIC')),
    -- 외부 도안은 원작 스냅샷을 만들지 않는다
    CONSTRAINT ck_project_external_no_snapshot CHECK (external_pattern_id IS NULL OR pattern_snapshot IS NULL),
    -- 핵심 불변식: 공개 로그를 가진 니팅로그는 반드시 공개
    CONSTRAINT ck_project_public_invariant CHECK (visibility = 'PUBLIC' OR public_log_count = 0),
    CONSTRAINT fk_project_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_project_selling FOREIGN KEY (selling_pattern_id) REFERENCES selling_pattern (id),
    CONSTRAINT fk_project_external FOREIGN KEY (external_pattern_id) REFERENCES external_pattern (id)
);

-- ---------------------------------------------------------------------
-- 재료 하위 테이블 (실·바늘·게이지 다중 행) + 니팅로그 대표 이미지
-- input_source: 수기(MANUAL)/AI 변환(AI) 구분
-- ---------------------------------------------------------------------
CREATE TABLE project_yarn (
    id           BIGSERIAL   PRIMARY KEY,
    project_id   BIGINT      NOT NULL,
    brand        VARCHAR(100),
    yarn_name    VARCHAR(100),
    color        VARCHAR(50),
    amount       VARCHAR(50),
    unit         VARCHAR(20),
    note         VARCHAR(200),
    sort_order   INT         NOT NULL DEFAULT 0,
    input_source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    CONSTRAINT ck_yarn_input_source CHECK (input_source IN ('MANUAL','AI')),
    CONSTRAINT fk_yarn_project FOREIGN KEY (project_id) REFERENCES knitting_project (id) ON DELETE CASCADE
);

CREATE TABLE project_needle (
    id           BIGSERIAL   PRIMARY KEY,
    project_id   BIGINT      NOT NULL,
    needle_type  VARCHAR(20),
    size_mm      NUMERIC(4,2),
    length_cm    INT,
    note         VARCHAR(200),
    sort_order   INT         NOT NULL DEFAULT 0,
    input_source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    CONSTRAINT ck_needle_input_source CHECK (input_source IN ('MANUAL','AI')),
    CONSTRAINT fk_needle_project FOREIGN KEY (project_id) REFERENCES knitting_project (id) ON DELETE CASCADE
);

CREATE TABLE project_gauge (
    id              BIGSERIAL   PRIMARY KEY,
    project_id      BIGINT      NOT NULL,
    stitches        NUMERIC(5,2),
    rows            NUMERIC(5,2),
    swatch_width_cm NUMERIC(5,2),
    swatch_height_cm NUMERIC(5,2),
    needle_size_mm  NUMERIC(4,2),
    measured_stage  VARCHAR(20),
    sort_order      INT         NOT NULL DEFAULT 0,
    input_source    VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    CONSTRAINT ck_gauge_measured_stage CHECK (measured_stage IS NULL OR measured_stage IN ('SWATCH','BODY')),
    CONSTRAINT ck_gauge_input_source CHECK (input_source IN ('MANUAL','AI')),
    CONSTRAINT fk_gauge_project FOREIGN KEY (project_id) REFERENCES knitting_project (id) ON DELETE CASCADE
);

CREATE TABLE project_image (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT    NOT NULL,
    file_id    BIGINT    NOT NULL,
    sort_order INT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_project_image_sort CHECK (sort_order BETWEEN 0 AND 6),  -- 대표 이미지 최대 7개(0~6)
    CONSTRAINT fk_project_image_project FOREIGN KEY (project_id) REFERENCES knitting_project (id) ON DELETE CASCADE,
    CONSTRAINT fk_project_image_file FOREIGN KEY (file_id) REFERENCES file_asset (id)
);

-- ---------------------------------------------------------------------
-- 인덱스 (ERD 11)
-- ---------------------------------------------------------------------
CREATE INDEX idx_project_group    ON knitting_project (user_id, deleted_at, selling_pattern_id);  -- 코잇기 그룹
CREATE INDEX idx_project_filter   ON knitting_project (user_id, status, created_at DESC);
CREATE INDEX idx_project_public   ON knitting_project (selling_pattern_id, visibility, deleted_at); -- 공개 니팅로그 수
CREATE INDEX idx_external_user    ON external_pattern (user_id);
CREATE INDEX idx_yarn_project     ON project_yarn (project_id);
CREATE INDEX idx_needle_project   ON project_needle (project_id);
CREATE INDEX idx_gauge_project    ON project_gauge (project_id);
CREATE INDEX idx_project_image_p  ON project_image (project_id);
