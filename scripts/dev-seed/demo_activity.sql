-- =====================================================================
-- 데모 활동 시드: 도안별 니팅로그(실·바늘·게이지) + 베스트셀러용 구매.
--   · '니팅로그로 보는 실제 제작' 집계와 홈 베스트셀러 순위를 데모용으로 채운다.
--   · 도안은 '제목' 기준으로 찾으므로 dev DB 를 리셋해도 재현 가능하다(해당 제목 도안이 있을 때만).
--   · 이미 충분히 시드된 도안(로그 5+/구매 5+)은 건너뛴다 → 재실행해도 크게 중복되지 않는다.
-- 실행: docker exec -i <pg> psql -U koitda -d koitda < scripts/dev-seed/demo_activity.sql
-- 주의: 개발/데모 전용. 운영 DB 에서 실행 금지.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1) 니팅로그 + 실·바늘·게이지 (도안 특성에 맞춘 대표값 + 소수 변형)
-- ---------------------------------------------------------------------
DO $$
DECLARE
  cfg RECORD; i INT; uid BIGINT; pid BIGINT; pat_id BIGINT; craft TEXT; ntype TEXT; st TEXT;
  ytier INT; ntier INT; gtier INT; existing INT;
  yb TEXT[]; yn TEXT[]; nmv NUMERIC[]; gsv NUMERIC[]; grv NUMERIC[];
BEGIN
  FOR cfg IN
    SELECT * FROM (VALUES
      ('Summer Lotus',                    10),
      ('00:00',                           12),
      ('여름별꽃 레이어드 스커트',        11),
      ('리본 뷔스티에',                    9),
      ('허그데이 스웨터 (hugday sweater)', 11),
      ('믹스탑 Mix Top',                  10),
      ('올드패션드 가디건',                9),
      ('Sailor Slippers',                  8),
      ('하트 윙 완드',                     9)
    ) AS t(title, nlogs)
  LOOP
    SELECT id, craft_type INTO pat_id, craft
      FROM selling_pattern WHERE title = cfg.title AND product_status = 'APPROVED' AND deleted_at IS NULL
      ORDER BY id LIMIT 1;
    IF pat_id IS NULL THEN CONTINUE; END IF;

    SELECT count(*) INTO existing FROM knitting_project WHERE selling_pattern_id = pat_id AND deleted_at IS NULL;
    IF existing >= 5 THEN CONTINUE; END IF; -- 이미 시드됨

    ntype := CASE WHEN craft = 'CROCHET' THEN '코바늘' ELSE '대바늘' END;

    -- 도안별 실/바늘/게이지 후보(1=대표, 2·3=변형)
    IF cfg.title = 'Summer Lotus' THEN
      yb := ARRAY['삼호','니뜨','랑']; yn := ARRAY['여름코튼','린넨블렌드','코튼소프트'];
      nmv := ARRAY[5.0,4.5,5.5]; gsv := ARRAY[15,16,14]; grv := ARRAY[21,22,20];
    ELSIF cfg.title = '00:00' THEN
      yb := ARRAY['필콥','로얄','랑']; yn := ARRAY['메리노','베이비알파카','메리노소프트'];
      nmv := ARRAY[4.0,3.5,4.5]; gsv := ARRAY[28,27,29]; grv := ARRAY[33,32,34];
    ELSIF cfg.title = '여름별꽃 레이어드 스커트' THEN
      yb := ARRAY['낙양모사','삼호','니뜨']; yn := ARRAY['코튼세라','여름코튼','린넨'];
      nmv := ARRAY[4.0,3.5,4.5]; gsv := ARRAY[24,23,25]; grv := ARRAY[36,35,37];
    ELSIF cfg.title = '리본 뷔스티에' THEN
      yb := ARRAY['삼호','니뜨','랑']; yn := ARRAY['레이스코튼','여름실','코튼'];
      nmv := ARRAY[3.0,2.5,3.5]; gsv := ARRAY[20,19,21]; grv := ARRAY[14,13,15];
    ELSIF cfg.title = '허그데이 스웨터 (hugday sweater)' THEN
      yb := ARRAY['필콥','로얄','랑']; yn := ARRAY['메리노','베이비알파카','소프트울'];
      nmv := ARRAY[4.0,3.5,4.5]; gsv := ARRAY[23,22,24]; grv := ARRAY[38,37,39];
    ELSIF cfg.title = '믹스탑 Mix Top' THEN
      yb := ARRAY['삼호','니뜨','랑']; yn := ARRAY['여름코튼','린넨','코튼블렌드'];
      nmv := ARRAY[4.5,4.0,5.0]; gsv := ARRAY[18,17,19]; grv := ARRAY[31,30,32];
    ELSIF cfg.title = '올드패션드 가디건' THEN
      yb := ARRAY['필콥','낙양모사','랑']; yn := ARRAY['메리노','울세라','트위드'];
      nmv := ARRAY[5.0,4.5,5.5]; gsv := ARRAY[19,18,20]; grv := ARRAY[26,25,27];
    ELSIF cfg.title = 'Sailor Slippers' THEN
      yb := ARRAY['랑','필콥','니뜨']; yn := ARRAY['청키울','벌키','울로빙'];
      nmv := ARRAY[8.0,7.0,9.0]; gsv := ARRAY[15,14,16]; grv := ARRAY[26,25,27];
    ELSE -- 하트 윙 완드
      yb := ARRAY['삼호','니뜨','랑']; yn := ARRAY['레이스코튼','코튼','아크릴'];
      nmv := ARRAY[4.5,4.0,5.0]; gsv := ARRAY[20,19,21]; grv := ARRAY[28,27,29];
    END IF;

    FOR i IN 1..cfg.nlogs LOOP
      INSERT INTO users(nickname) VALUES ('데모제작러_' || pat_id || '_' || i) RETURNING id INTO uid;
      INSERT INTO user_role(user_id, role) VALUES (uid, 'USER');
      st := CASE (i % 5) WHEN 0 THEN 'FO' WHEN 1 THEN 'FO' WHEN 2 THEN 'WIP' WHEN 3 THEN 'CO' ELSE 'UFO' END;

      INSERT INTO knitting_project(user_id, selling_pattern_id, display_title, status, visibility, created_at, updated_at)
      VALUES (uid, pat_id, cfg.title, st, 'PRIVATE', now() - make_interval(days => (random()*120)::int), now())
      RETURNING id INTO pid;

      ytier := CASE WHEN i*100 <= cfg.nlogs*55 THEN 1 WHEN i%2=0 THEN 2 ELSE 3 END;
      ntier := CASE WHEN i*100 <= cfg.nlogs*60 THEN 1 WHEN i%3=0 THEN 2 ELSE 3 END;
      gtier := CASE WHEN i*100 <= cfg.nlogs*50 THEN 1 WHEN i%2=0 THEN 2 ELSE 3 END;

      INSERT INTO project_yarn(project_id, brand, yarn_name, amount, unit, sort_order)
      VALUES (pid, yb[ytier], yn[ytier], (2 + (i%3))::text, '볼', 0);
      INSERT INTO project_needle(project_id, needle_type, size_mm, sort_order)
      VALUES (pid, ntype, nmv[ntier], 0);
      INSERT INTO project_gauge(project_id, stitches, rows, needle_size_mm, measured_stage, sort_order)
      VALUES (pid, gsv[gtier], grv[gtier], nmv[ntier], 'SWATCH', 0);
    END LOOP;
  END LOOP;
END $$;

-- ---------------------------------------------------------------------
-- 2) 베스트셀러 구매 부스트 (구매 1건 = 주문 PAID + 주문항목 + 라이브러리)
--    특정 도안들의 구매수를 올려 홈 베스트셀러 순위를 다양하게 만든다.
-- ---------------------------------------------------------------------
DO $$
DECLARE
  cfg RECORD; i INT; buyer BIGINT; oid BIGINT; oiid BIGINT; seq INT := 0;
  pat_id BIGINT; sellerId BIGINT; price BIGINT; existing INT;
BEGIN
  FOR cfg IN
    SELECT * FROM (VALUES
      ('Summer Lotus',                     8),
      ('여름별꽃 레이어드 스커트',         7),
      ('00:00',                            6),
      ('허그데이 스웨터 (hugday sweater)',  6),
      ('믹스탑 Mix Top',                   5),
      ('리본 뷔스티에',                     5),
      ('올드패션드 가디건',                 4)
    ) AS t(title, target_purchases)
  LOOP
    SELECT id, seller_id, COALESCE(sale_price, regular_price, 0)
      INTO pat_id, sellerId, price
      FROM selling_pattern WHERE title = cfg.title AND product_status = 'APPROVED' AND deleted_at IS NULL
      ORDER BY id LIMIT 1;
    IF pat_id IS NULL THEN CONTINUE; END IF;

    SELECT count(*) INTO existing FROM pattern_library WHERE pattern_id = pat_id AND revoked_at IS NULL;
    -- 목표 구매수까지만 채운다(부족분만 추가).
    FOR i IN 1..GREATEST(cfg.target_purchases - existing, 0) LOOP
      seq := seq + 1;
      INSERT INTO users(nickname) VALUES ('데모구매러_' || pat_id || '_' || i) RETURNING id INTO buyer;
      INSERT INTO user_role(user_id, role) VALUES (buyer, 'USER');
      INSERT INTO customer_order(order_no, buyer_id, total_amount, payment_amount, order_status, ordered_at)
      VALUES ('DEMOSEED-' || pat_id || '-' || i || '-' || seq, buyer, price, price, 'PAID',
              now() - make_interval(days => (random()*60)::int))
      RETURNING id INTO oid;
      INSERT INTO order_item(order_id, pattern_id, seller_id, unit_price, item_amount)
      VALUES (oid, pat_id, sellerId, price, price) RETURNING id INTO oiid;
      INSERT INTO pattern_library(user_id, pattern_id, order_item_id, purchased_at)
      VALUES (buyer, pat_id, oiid, now() - make_interval(days => (random()*60)::int));
    END LOOP;
  END LOOP;
END $$;
