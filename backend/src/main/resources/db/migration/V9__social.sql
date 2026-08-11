-- =====================================================================
-- V9 : 소셜·신고 (요구사항 SOCIAL-001~005, REPORT-001~003, REVIEW-009)
-- ERD v1.3 도메인 6. 좋아요·댓글·신고는 대상 다형성(target_type, target_id)으로 다룬다.
--   target_type: POST(오늘의 로그·실타래) / REVIEW(도안 리뷰) / COMMENT / USER
-- 핵심 불변식은 DB 제약으로 강제한다.
--   · 계정당 좋아요 1회      → PK (user_id, target_type, target_id)
--   · 자기 자신 팔로우 금지   → CHECK (follower_id <> following_id)
--   · 같은 콘텐츠 중복 신고 X → UNIQUE (reporter_id, target_type, target_id)
-- =====================================================================

-- 좋아요(SOCIAL-001) — 복합 PK 가 '계정당 1회'를 보장한다.
CREATE TABLE post_like (
    user_id     BIGINT      NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id   BIGINT      NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_post_like PRIMARY KEY (user_id, target_type, target_id),
    CONSTRAINT ck_like_target CHECK (target_type IN ('POST','REVIEW','COMMENT')),
    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_like_target ON post_like (target_type, target_id);

-- 댓글(SOCIAL-002·003)
CREATE TABLE comment (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id   BIGINT      NOT NULL,
    content     TEXT        NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT ck_comment_target CHECK (target_type IN ('POST','REVIEW')),
    CONSTRAINT ck_comment_status CHECK (status IN ('NORMAL','HIDDEN')),
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_comment_target ON comment (target_type, target_id, deleted_at);

-- 팔로우(SOCIAL-004) — 복합 PK + 자기참조 금지 CHECK
CREATE TABLE follow (
    follower_id  BIGINT      NOT NULL,
    following_id BIGINT      NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_follow PRIMARY KEY (follower_id, following_id),
    CONSTRAINT ck_follow_not_self CHECK (follower_id <> following_id),
    CONSTRAINT fk_follow_follower  FOREIGN KEY (follower_id)  REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_follow_following FOREIGN KEY (following_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_follow_following ON follow (following_id);

-- 신고(REPORT-001~003) — UNIQUE 가 '계정당 콘텐츠 1회'를 보장한다.
CREATE TABLE report (
    id          BIGSERIAL   PRIMARY KEY,
    reporter_id BIGINT      NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id   BIGINT      NOT NULL,
    reason_code VARCHAR(30),
    detail      TEXT,
    is_valid    BOOLEAN     NOT NULL DEFAULT true,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolved_by BIGINT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_report_once UNIQUE (reporter_id, target_type, target_id),
    CONSTRAINT ck_report_target CHECK (target_type IN ('POST','REVIEW','COMMENT','USER')),
    CONSTRAINT ck_report_status CHECK (status IN ('PENDING','CONFIRMED','DISMISSED')),
    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES users (id)
);
CREATE INDEX idx_report_target ON report (target_type, target_id);

-- 신고 집계·자동 숨김(REPORT-002·003) — 대상당 한 행으로 유효 신고 수를 모은다.
CREATE TABLE content_moderation (
    id                  BIGSERIAL   PRIMARY KEY,
    target_type         VARCHAR(20) NOT NULL,
    target_id           BIGINT      NOT NULL,
    valid_report_count  INT         NOT NULL DEFAULT 0,
    is_flagged          BOOLEAN     NOT NULL DEFAULT false,  -- 3건 이상: 관리자 확인 대상
    is_auto_hidden      BOOLEAN     NOT NULL DEFAULT false,  -- 10건 이상: 자동 숨김
    hidden_at           TIMESTAMPTZ,
    restored_by         BIGINT,
    restored_at         TIMESTAMPTZ,
    CONSTRAINT uq_moderation_target UNIQUE (target_type, target_id)
);
