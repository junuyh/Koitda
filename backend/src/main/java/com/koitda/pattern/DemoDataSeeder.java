package com.koitda.pattern;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개발(dev 프로파일) 전용 데모 카탈로그 시드.
 * 판매자 등록 화면이 아직 없으므로, 목록·상세·위시를 실제로 확인하기 위한 샘플 데이터를 넣는다.
 * selling_pattern 이 비어 있을 때만 1회 삽입하여 재기동에도 중복되지 않는다(멱등).
 * 테스트에는 dev 프로파일이 없으므로 실행되지 않는다.
 */
@Component
@Profile("dev")
public class DemoDataSeeder implements CommandLineRunner {

	private final JdbcTemplate jdbc;

	public DemoDataSeeder(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	@Transactional
	public void run(String... args) {
		Integer count = jdbc.queryForObject("SELECT count(*) FROM selling_pattern", Integer.class);
		if (count != null && count > 0) {
			return; // 이미 시드됨
		}

		Long sellerUserId = jdbc.queryForObject(
				"INSERT INTO users(nickname) VALUES ('데모공방') RETURNING id", Long.class);
		Long sellerId = jdbc.queryForObject(
				"INSERT INTO seller_profile(user_id, brand_name) VALUES (?, '데모공방') RETURNING id",
				Long.class, sellerUserId);

		long catSweater = category("스웨터·가디건", 1);
		long catHat = category("모자", 2);
		long catBag = category("가방", 3);
		long catSocks = category("양말", 4);

		seedPattern(sellerId, sellerUserId, "올드패션 가디건", "라라니트", catSweater, "KNIT", "중급", 18000);
		seedPattern(sellerId, sellerUserId, "포근한 라글란 스웨터", "코코뜨개", catSweater, "KNIT", "초급", 15000);
		seedPattern(sellerId, sellerUserId, "데일리 버킷햇", "모자공방", catHat, "CROCHET", "초급", 8000);
		seedPattern(sellerId, sellerUserId, "그래니 네트백", "여름실", catBag, "CROCHET", "중급", 12000);
		seedPattern(sellerId, sellerUserId, "기본 삭스", "발편한니트", catSocks, "KNIT", "고급", 9000);
		seedPattern(sellerId, sellerUserId, "케이블 비니", "겨울손", catHat, "KNIT", "중급", 7000);
	}

	private long category(String name, int sortOrder) {
		return jdbc.queryForObject(
				"INSERT INTO pattern_category(name, sort_order) VALUES (?, ?) RETURNING id",
				Long.class, name, sortOrder);
	}

	private void seedPattern(long sellerId, long uploaderId, String title, String designer,
			long categoryId, String craft, String difficulty, long price) {
		Long patternId = jdbc.queryForObject("""
				INSERT INTO selling_pattern
				  (seller_id, title, designer_name, category_id, craft_type, difficulty,
				   language, regular_price, sale_price, product_status, published_at,
				   gauge_info, size_info, description)
				VALUES (?, ?, ?, ?, ?, ?, 'ko', ?, ?, 'APPROVED', now(),
				  '{"stitches":22,"rows":30,"swatchWidthCm":10,"swatchHeightCm":10,"needleSizeMm":4.5}'::jsonb,
				  '{"sizes":[{"label":"1 (S)","castOnStitches":132,"measurements":{"chestCm":96,"lengthCm":58,"sleeveLengthCm":46}},{"label":"2 (M)","castOnStitches":148,"measurements":{"chestCm":106,"lengthCm":60,"sleeveLengthCm":47}}]}'::jsonb,
				  '샘플 도안 상세 설명입니다. 실제 판매 도안 등록 기능은 이후 슬라이스에서 추가됩니다.')
				RETURNING id
				""", Long.class, sellerId, title, designer, categoryId, craft, difficulty, price, price);

		Long fileId = jdbc.queryForObject("""
				INSERT INTO file_asset(uploader_id, usage_type, storage_key, upload_status)
				VALUES (?, 'PATTERN_IMAGE', ?, 'COMPLETED')
				RETURNING id
				""", Long.class, uploaderId, "demo/patterns/" + patternId + ".jpg");

		jdbc.update("""
				INSERT INTO selling_pattern_image(pattern_id, file_id, sort_order, is_thumbnail)
				VALUES (?, ?, 0, true)
				""", patternId, fileId);
	}
}
