-- =====================================================================
-- V1 : 인증·역할·사용자·공통 파일  (요구사항 AUTH-001~006, DATA-001·005)
-- ERD v1.3 도메인 1 기준. 이 슬라이스 하나를 세로로 완성하는 데 필요한 최소 테이블만 담는다.
--
-- [문서와의 차이] ERD는 테이블명을 USER 로 표기하나, USER 는 PostgreSQL 예약어라
-- 따옴표 없이 CREATE TABLE user 가 불가능하다. 물리 테이블명은 users 로 둔다.
-- (나머지 테이블은 ERD대로 단수 유지)
-- =====================================================================

-- ---------------------------------------------------------------------
-- FILE_ASSET : 공통 파일 메타데이터 (DB엔 키/메타만, 실체는 객체 저장소)
-- users 와 상호 참조(순환 FK)이므로 uploader_id FK 는 users 생성 후 마지막에 건다.
-- ---------------------------------------------------------------------
CREATE TABLE file_asset (
    id            BIGSERIAL     PRIMARY KEY,
    uploader_id   BIGINT        NOT NULL,
    usage_type    VARCHAR(30)   NOT NULL,
    original_name VARCHAR(255),
    content_type  VARCHAR(100),
    byte_size     BIGINT,
    storage_key   VARCHAR(500)  NOT NULL,
    thumbnail_key VARCHAR(500),
    upload_status VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT ck_file_asset_usage_type
        CHECK (usage_type IN ('PROFILE','PATTERN_IMAGE','PATTERN_PDF',
                              'PROJECT_IMAGE','POST_IMAGE','REVIEW_IMAGE','SELLER_EVIDENCE')),
    CONSTRAINT ck_file_asset_upload_status
        CHECK (upload_status IN ('PENDING','COMPLETED'))
);

-- ---------------------------------------------------------------------
-- USERS(회원)
--   email/password_hash : 탈퇴 시 NULL 로 파기 (DATA-001)  → NULL 허용
--   nickname            : 탈퇴 후에도 유지·재사용 차단      → UNIQUE NOT NULL
--   point_balance       : POINT_TRANSACTION 합계의 캐시. 진실의 출처는 거래내역
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                      BIGSERIAL    PRIMARY KEY,
    email                   VARCHAR(255),
    password_hash           VARCHAR(255),
    nickname                VARCHAR(30)  NOT NULL,
    profile_image_id        BIGINT,
    intro                   VARCHAR(200),
    status                  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    pattern_lock_hash       VARCHAR(255),
    pattern_lock_updated_at TIMESTAMPTZ,
    point_balance           INT          NOT NULL DEFAULT 0,
    withdrawn_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email    UNIQUE (email),      -- NULL 은 중복 허용(탈퇴자 다수 가능)
    CONSTRAINT uq_users_nickname UNIQUE (nickname),
    CONSTRAINT ck_users_status   CHECK (status IN ('ACTIVE','SUSPENDED','WITHDRAWN')),
    CONSTRAINT ck_users_point_nonneg CHECK (point_balance >= 0),
    CONSTRAINT fk_users_profile_image
        FOREIGN KEY (profile_image_id) REFERENCES file_asset (id)
);

-- 순환 FK 마무리 : 파일 업로더 → 회원
ALTER TABLE file_asset
    ADD CONSTRAINT fk_file_asset_uploader
        FOREIGN KEY (uploader_id) REFERENCES users (id);

-- ---------------------------------------------------------------------
-- USER_ROLE : 역할 다중 보유 (AUTH-005)
--   한 컬럼 role 로 두지 않는다. USER·SELLER·ADMIN 을 행으로 쌓아 겸직을 표현한다.
-- ---------------------------------------------------------------------
CREATE TABLE user_role (
    user_id    BIGINT      NOT NULL,
    role       VARCHAR(20) NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_role PRIMARY KEY (user_id, role),
    CONSTRAINT ck_user_role_role CHECK (role IN ('USER','SELLER','ADMIN')),
    CONSTRAINT fk_user_role_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- TERMS_AGREEMENT : 약관 동의 이력 (AUTH-006). 버전과 동의 시각을 함께 보존
-- ---------------------------------------------------------------------
CREATE TABLE terms_agreement (
    id            BIGSERIAL   PRIMARY KEY,
    user_id       BIGINT      NOT NULL,
    terms_type    VARCHAR(30) NOT NULL,
    terms_version VARCHAR(20) NOT NULL,
    is_agreed     BOOLEAN     NOT NULL,
    agreed_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_terms_type CHECK (terms_type IN ('SERVICE','PRIVACY','MARKETING','SELLER')),
    CONSTRAINT fk_terms_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- 인덱스 : FK 컬럼 조회 성능 (PK 로 이미 커버되는 것은 생략)
-- ---------------------------------------------------------------------
CREATE INDEX idx_file_asset_uploader ON file_asset (uploader_id);
CREATE INDEX idx_terms_agreement_user ON terms_agreement (user_id);
