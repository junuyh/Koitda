package com.koitda.post;

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

/** V4 content_post 스키마와 유형별 CHECK 제약 검증. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class V4ContentPostMigrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void content_post_테이블이_생성된다() {
		List<String> tables = jdbc.queryForList(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
				String.class);
		assertTrue(tables.contains("content_post"), "생성된 테이블: " + tables);
	}

	@Test
	void PROJECT_LOG은_project_id가_없으면_거부된다() {
		long userId = jdbc.queryForObject("INSERT INTO users(nickname) VALUES ('로그러') RETURNING id", Long.class);
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
				INSERT INTO content_post(user_id, post_type, display_title, log_date)
				VALUES (?, 'PROJECT_LOG', '오늘의 로그', CURRENT_DATE)
				""", userId));
	}

	@Test
	void FREE_POST은_니팅상태를_가질_수_없다() {
		long userId = jdbc.queryForObject("INSERT INTO users(nickname) VALUES ('실타래러') RETURNING id", Long.class);
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
				INSERT INTO content_post(user_id, post_type, display_title, knitting_status)
				VALUES (?, 'FREE_POST', '자유글', 'CO')
				""", userId));
	}
}
