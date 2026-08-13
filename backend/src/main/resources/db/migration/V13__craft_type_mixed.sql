-- 뜨개 방식에 '혼합(MIXED)' 추가 — 대바늘·코바늘 혼용 도안.
ALTER TABLE selling_pattern DROP CONSTRAINT ck_pattern_craft_type;
ALTER TABLE selling_pattern ADD CONSTRAINT ck_pattern_craft_type
    CHECK (craft_type IN ('CROCHET', 'KNIT', 'MIXED'));

ALTER TABLE external_pattern DROP CONSTRAINT ck_external_craft_type;
ALTER TABLE external_pattern ADD CONSTRAINT ck_external_craft_type
    CHECK (craft_type IS NULL OR craft_type IN ('CROCHET', 'KNIT', 'MIXED'));
