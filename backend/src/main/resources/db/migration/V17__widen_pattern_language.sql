-- =====================================================================
-- V17 : selling_pattern.language 폭 확장(varchar(10) → varchar(100)).
-- 도안 등록 시 언어를 여러 개(쉼표 구분)로 받는데 10자 제한이라 저장이 실패했다
--   (예: "한국어, English" → 'value too long for type character varying(10)').
-- 여러 언어 표기를 넉넉히 담도록 100자로 늘린다.
-- =====================================================================
ALTER TABLE selling_pattern ALTER COLUMN language TYPE VARCHAR(100);
