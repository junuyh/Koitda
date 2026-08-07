package com.koitda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * V1 마이그레이션이 실제 PostgreSQL(Testcontainers)에 적용되고,
 * 핵심 DB 제약이 애플리케이션이 아닌 DB 레벨에서 강제되는지 검증한다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class V1MigrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void 네개_테이블이_생성된다() {
		List<String> tables = jdbc.queryForList(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
				String.class);
		assertTrue(tables.containsAll(List.of("users", "user_role", "terms_agreement", "file_asset")),
				"생성된 테이블: " + tables);
	}

	@Test
	void 포인트잔액_음수는_DB가_거부한다() {
		Long id = jdbc.queryForObject(
				"INSERT INTO users(nickname) VALUES ('point_tester') RETURNING id", Long.class);
		assertThrows(DataIntegrityViolationException.class,
				() -> jdbc.update("UPDATE users SET point_balance = -1 WHERE id = ?", id));
	}

	@Test
	void 한_계정이_역할을_겸직할_수_있다() {
		Long id = jdbc.queryForObject(
				"INSERT INTO users(nickname) VALUES ('role_tester') RETURNING id", Long.class);
		jdbc.update("INSERT INTO user_role(user_id, role) VALUES (?, 'USER')", id);
		jdbc.update("INSERT INTO user_role(user_id, role) VALUES (?, 'SELLER')", id);

		Integer count = jdbc.queryForObject(
				"SELECT count(*) FROM user_role WHERE user_id = ?", Integer.class, id);
		assertEquals(2, count);
	}
}
