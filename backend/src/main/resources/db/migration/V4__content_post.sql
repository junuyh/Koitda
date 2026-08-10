-- =====================================================================
-- V4 : 오늘의 로그 + 실타래 (요구사항 POST-001~012, FREE-001~004)
-- ERD v1.3 CONTENT_POST. 하나의 테이블이 PROJECT_LOG(오늘의 로그)와 FREE_POST(실타래)를 담는다.
-- 이번 슬라이스는 오늘의 로그(PROJECT_LOG)와 상태 파생에 집중한다.
-- =====================================================================

CREATE TABLE content_post (
    id                BIGSERIAL    PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    post_type         VARCHAR(20)  NOT NULL,
    project_id        BIGINT,                    -- PROJECT_LOG 일 때만
    title             VARCHAR(200),
    display_title     VARCHAR(200) NOT NULL,
    title_sequence    INT,
    log_date          DATE,                      -- 기록일. 정렬·최신 판정 기준
    knitting_status   VARCHAR(20),               -- PROJECT_LOG 전용
    content_document  JSONB,                     -- TipTap JSON
    content_text      TEXT,                      -- 검색용 평문
    cover_file_id     BIGINT,
    visibility        VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE',
    moderation_status VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    moderated_at      TIMESTAMPTZ,
    moderated_by      BIGINT,
    like_count        INT          NOT NULL DEFAULT 0,
    comment_count     INT          NOT NULL DEFAULT 0,
    view_count        INT          NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,
    purge_at          TIMESTAMPTZ,
    CONSTRAINT ck_post_type CHECK (post_type IN ('PROJECT_LOG','FREE_POST')),
    CONSTRAINT ck_post_visibility CHECK (visibility IN ('PRIVATE','PUBLIC')),
    CONSTRAINT ck_post_moderation CHECK (moderation_status IN ('NORMAL','AUTO_HIDDEN','ADMIN_HIDDEN','RESTORED')),
    CONSTRAINT ck_post_status CHECK (knitting_status IS NULL OR knitting_status IN ('PLANNED','CO','WIP','UFO','FO')),
    -- 유형별 필수/금지 조합
    CONSTRAINT ck_post_projectlog_has_project CHECK (post_type <> 'PROJECT_LOG' OR project_id IS NOT NULL),
    CONSTRAINT ck_post_freepost_no_project    CHECK (post_type <> 'FREE_POST'   OR project_id IS NULL),
    CONSTRAINT ck_post_freepost_no_status     CHECK (post_type <> 'FREE_POST'   OR knitting_status IS NULL),
    CONSTRAINT ck_post_projectlog_has_logdate CHECK (post_type <> 'PROJECT_LOG' OR log_date IS NOT NULL),
    CONSTRAINT fk_post_user    FOREIGN KEY (user_id)    REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_project FOREIGN KEY (project_id) REFERENCES knitting_project (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_cover   FOREIGN KEY (cover_file_id) REFERENCES file_asset (id)
);

-- 로그 목록·최신 판정(상태 파생) 기준 인덱스
CREATE INDEX idx_post_project_log ON content_post (project_id, log_date DESC, created_at DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_post_feed        ON content_post (visibility, post_type, created_at DESC);
