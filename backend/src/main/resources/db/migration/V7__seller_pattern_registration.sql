-- =====================================================================
-- V7 : 판매자 도안 등록 (요구사항 SELLER-002·003, PATTERN-011, ADMIN-001)
-- SELLING_PATTERN 은 V2 에 이미 있다. 여기서는 '심사(승인/반려) 감사' 컬럼만 추가한다.
--   product_status 전이:  DRAFT → PENDING(제출) → APPROVED/REJECTED(관리자 심사)
--   reviewed_by/at·rejection_reason 은 관리자만 갱신하는 심사 결과(ADMIN-001: 처리자·일시·사유 저장).
--   visibility 와 달리 이 축은 '심사 주체 = 관리자'로 고정 — 작성자가 되돌릴 수 없다(권한 주체 분리 원칙).
-- =====================================================================

ALTER TABLE selling_pattern
    ADD COLUMN reviewed_by      BIGINT,
    ADD COLUMN reviewed_at      TIMESTAMPTZ,
    ADD COLUMN rejection_reason TEXT,
    ADD CONSTRAINT fk_pattern_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id);

-- 판매자 '내 도안 목록'(GET /seller/patterns): 소유자 + 상태 필터 + 최신순
CREATE INDEX idx_pattern_seller ON selling_pattern (seller_id, product_status, updated_at DESC);
