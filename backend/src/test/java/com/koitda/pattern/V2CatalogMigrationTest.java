package com.koitda.pattern;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.koitda.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/** V2 카탈로그 스키마가 적용되고 핵심 제약이 DB 레벨에서 강제되는지 검증. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class V2CatalogMigrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	/** user → seller_profile → pattern 을 만들고 pattern_id 를 돌려준다. */
	private long seedPattern(String nick, String brand, String craftType) {
		Long userId = jdbc.queryForObject(
				"INSERT INTO users(nickname) VALUES (?) RETURNING id", Long.class, nick);
		Long sellerId = jdbc.queryForObject(
				"INSERT INTO seller_profile(user_id, brand_name) VALUES (?, ?) RETURNING id",
				Long.class, userId, brand);
		return jdbc.queryForObject(
				"INSERT INTO selling_pattern(seller_id, title, craft_type) VALUES (?, ?, ?) RETURNING id",
				Long.class, sellerId, "샘플 도안", craftType);
	}

	@Test
	void 카탈로그_테이블이_생성된다() {
		List<String> tables = jdbc.queryForList(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
				String.class);
		assertTrue(tables.containsAll(List.of(
				"pattern_category", "seller_profile", "selling_pattern",
				"selling_pattern_image", "wish")), "생성된 테이블: " + tables);
	}

	@Test
	void 위시는_계정당_도안_1회만_가능하다() {
		long patternId = seedPattern("위시셀러", "위시브랜드", "KNIT");
		Long wisherId = jdbc.queryForObject(
				"INSERT INTO users(nickname) VALUES ('위시러') RETURNING id", Long.class);

		jdbc.update("INSERT INTO wish(user_id, pattern_id) VALUES (?, ?)", wisherId, patternId);

		// 같은 계정이 같은 도안을 다시 위시 → 복합 PK 위반
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("INSERT INTO wish(user_id, pattern_id) VALUES (?, ?)", wisherId, patternId));
	}

	@Test
	void 뜨개방식은_CROCHET_또는_KNIT_만_허용한다() {
		assertThrows(DataIntegrityViolationException.class,
				() -> seedPattern("잘못셀러", "잘못브랜드", "SEWING"));
	}
}
