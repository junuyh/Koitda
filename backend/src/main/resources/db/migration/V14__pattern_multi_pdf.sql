-- 도안 PDF 여러 개 등록. 기존 current_file_id(대표/다운로드용)는 유지하고 목록을 별도 보관.
ALTER TABLE selling_pattern ADD COLUMN pdf_file_ids JSONB;
