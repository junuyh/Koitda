-- =====================================================================
-- V11 : 도안 상세 설명 블록 문서 (SELLER-003, 화면 피드백 ⑥)
-- 상세 설명을 블로그(블록) 형식으로 저장한다. 본문은 TipTap JSON(description_document),
-- description(TEXT)은 검색·미리보기용 평문 추출값으로 유지한다.
-- =====================================================================

ALTER TABLE selling_pattern ADD COLUMN description_document JSONB;
