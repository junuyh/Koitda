-- =====================================================================
-- V20 : 신고 처리(관리자 조치) 상태 추가. content_moderation 에 처리 결과를 기록해
--   관리자 신고 큐에서 '미처리(PENDING)'만 남기고, 숨김/무시/복원 이력을 남긴다(ADMIN-002).
--   PENDING(대기) / HIDDEN(숨김 처리) / DISMISSED(무시=유지) / RESTORED(숨김 해제)
-- =====================================================================
ALTER TABLE content_moderation ADD COLUMN resolution   VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE content_moderation ADD COLUMN resolved_by  BIGINT;
ALTER TABLE content_moderation ADD COLUMN resolved_at  TIMESTAMPTZ;
ALTER TABLE content_moderation ADD CONSTRAINT ck_moderation_resolution
    CHECK (resolution IN ('PENDING','HIDDEN','DISMISSED','RESTORED'));

CREATE INDEX idx_moderation_queue ON content_moderation (resolution, valid_report_count DESC);
