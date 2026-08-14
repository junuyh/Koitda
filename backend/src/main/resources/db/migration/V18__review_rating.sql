-- =====================================================================
-- V18 : 도안 리뷰 별점(rating). 리뷰 작성 시 1~5점 별점을 함께 받아 도안에 노출한다.
--   기존 리뷰는 별점이 없을 수 있으므로 NULL 허용. 값이 있으면 1~5 범위 강제(DB CHECK).
-- =====================================================================
ALTER TABLE pattern_review ADD COLUMN rating SMALLINT;
ALTER TABLE pattern_review ADD CONSTRAINT ck_review_rating
    CHECK (rating IS NULL OR (rating BETWEEN 1 AND 5));
