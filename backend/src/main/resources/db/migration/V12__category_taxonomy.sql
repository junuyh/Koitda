-- 카테고리 확장(도안 등록 개편). 기존 카테고리는 유지하고 트리를 추가한다.
-- 의류 > (상의 > 스웨터·가디건·풀오버·베스트·반소매·볼레로·집업 / 하의 > 치마·바지 / 드레스)
-- + 소품 최상위: 장갑·목도리·악세사리·기타 (양말·가방은 이미 존재)

-- 1) 최상위 추가(중복 방지)
INSERT INTO pattern_category (name, parent_id, sort_order)
SELECT v.name, NULL, v.so
FROM (VALUES ('의류', 10), ('장갑', 20), ('목도리', 21), ('악세사리', 22), ('기타', 99)) AS v(name, so)
WHERE NOT EXISTS (SELECT 1 FROM pattern_category c WHERE c.name = v.name AND c.parent_id IS NULL);

-- 2) 의류 하위 대분류
INSERT INTO pattern_category (name, parent_id, sort_order)
SELECT v.name, p.id, v.so
FROM (VALUES ('상의', 1), ('하의', 2), ('드레스', 3)) AS v(name, so)
JOIN pattern_category p ON p.name = '의류' AND p.parent_id IS NULL
WHERE NOT EXISTS (SELECT 1 FROM pattern_category c WHERE c.name = v.name AND c.parent_id = p.id);

-- 3) 상의 소분류
INSERT INTO pattern_category (name, parent_id, sort_order)
SELECT v.name, p.id, v.so
FROM (VALUES ('스웨터', 1), ('가디건', 2), ('풀오버', 3), ('베스트', 4), ('반소매', 5), ('볼레로', 6), ('집업', 7)) AS v(name, so)
JOIN pattern_category p ON p.name = '상의'
  AND p.parent_id = (SELECT id FROM pattern_category WHERE name = '의류' AND parent_id IS NULL)
WHERE NOT EXISTS (SELECT 1 FROM pattern_category c WHERE c.name = v.name AND c.parent_id = p.id);

-- 4) 하의 소분류
INSERT INTO pattern_category (name, parent_id, sort_order)
SELECT v.name, p.id, v.so
FROM (VALUES ('치마', 1), ('바지', 2)) AS v(name, so)
JOIN pattern_category p ON p.name = '하의'
  AND p.parent_id = (SELECT id FROM pattern_category WHERE name = '의류' AND parent_id IS NULL)
WHERE NOT EXISTS (SELECT 1 FROM pattern_category c WHERE c.name = v.name AND c.parent_id = p.id);
