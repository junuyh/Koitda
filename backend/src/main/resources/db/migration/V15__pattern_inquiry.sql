-- =====================================================================
-- V15 : 도안 문의(문의하기). 도안 상세 리뷰 하단에서 구매 전 사용자가 판매자에게 질문한다.
--   · 공개/비공개(is_private) — 비공개는 작성자·해당 도안 판매자·관리자만 본문 열람
--   · 판매자 "알림" 은 별도 알림 인프라 대신 미답변 문의 목록(폴링형 인박스)으로 제공
--   · pattern_id 는 참조(FK). 답변(answer)은 판매자가 작성하며 answered_by 로 감사한다.
-- 사용자 화면 표기: "문의하기". 요구사항 정의서에 없던 기능(사용자 확정 신규)으로 문서에도 반영.
-- =====================================================================
CREATE TABLE pattern_inquiry (
    id           BIGSERIAL   PRIMARY KEY,
    pattern_id   BIGINT      NOT NULL,                    -- 참조: 어떤 도안에 대한 문의인가
    user_id      BIGINT      NOT NULL,                    -- 질문 작성자
    is_private   BOOLEAN     NOT NULL DEFAULT false,      -- true 면 작성자·판매자·관리자만 본문 열람
    content      TEXT        NOT NULL,                    -- 질문 본문(평문)
    answer       TEXT,                                    -- 판매자 답변(없으면 미답변)
    answered_at  TIMESTAMPTZ,                             -- 답변 시각(미답변 판별 기준)
    answered_by  BIGINT,                                  -- 답변한 판매자 user_id(감사)
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at   TIMESTAMPTZ,                             -- 논리 삭제
    CONSTRAINT fk_inquiry_pattern FOREIGN KEY (pattern_id) REFERENCES selling_pattern (id),
    CONSTRAINT fk_inquiry_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    -- 답변이 있으면 답변 시각·답변자도 함께 채워져야 한다(부분 상태 방지).
    CONSTRAINT ck_inquiry_answer_consistent
        CHECK ((answer IS NULL AND answered_at IS NULL AND answered_by IS NULL)
            OR (answer IS NOT NULL AND answered_at IS NOT NULL AND answered_by IS NOT NULL))
);

-- 도안별 문의 목록(최신순). 삭제 필터 포함.
CREATE INDEX idx_inquiry_pattern ON pattern_inquiry (pattern_id, deleted_at, created_at DESC);
-- 판매자 인박스: 미답변 문의만 빠르게. (도안→판매자 조인은 selling_pattern 으로)
CREATE INDEX idx_inquiry_unanswered ON pattern_inquiry (pattern_id)
    WHERE answered_at IS NULL AND deleted_at IS NULL;
