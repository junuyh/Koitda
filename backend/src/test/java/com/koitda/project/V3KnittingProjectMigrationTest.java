package com.koitda.project;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

/** V3 스키마 적용 + 니팅로그 핵심 제약(도안 XOR·공개 불변식)이 DB 레벨에서 강제되는지 검증. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class V3KnittingProjectMigrationTest {

	@Autowired
	private JdbcTemplate jdbc;

	private long newUser(String nick) {
		return jdbc.queryForObject("INSERT INTO users(nickname) VALUES (?) RETURNING id", Long.class, nick);
	}

	private long newExternalPattern(long userId, String title) {
		return jdbc.queryForObject(
				"INSERT INTO external_pattern(user_id, title) VALUES (?, ?) RETURNING id",
				Long.class, userId, title);
	}

	@Test
	void 니팅로그_테이블들이_생성된다() {
		List<String> tables = jdbc.queryForList(
				"SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
				String.class);
		assertTrue(tables.containsAll(List.of("knitting_project", "external_pattern",
				"project_yarn", "project_needle", "project_gauge", "project_image")),
				"생성된 테이블: " + tables);
	}

	@Test
	void 외부도안_니팅로그는_정상_생성된다() {
		long userId = newUser("정상러");
		long extId = newExternalPattern(userId, "손뜨개 목도리");
		assertDoesNotThrow(() -> jdbc.update("""
				INSERT INTO knitting_project(user_id, external_pattern_id, display_title)
				VALUES (?, ?, '오늘의 목도리')
				""", userId, extId));
	}

	@Test
	void 도안과_외부도안을_모두_비우면_거부된다() {
		long userId = newUser("XOR러");
		// selling_pattern_id·external_pattern_id 둘 다 NULL → XOR 위반
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
				INSERT INTO knitting_project(user_id, display_title) VALUES (?, '무도안')
				""", userId));
	}

	@Test
	void 비공개인데_공개로그가_있으면_거부된다() {
		long userId = newUser("불변식러");
		long extId = newExternalPattern(userId, "비공개 작품");
		// visibility=PRIVATE 인데 public_log_count=1 → 핵심 불변식 위반
		assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("""
				INSERT INTO knitting_project(user_id, external_pattern_id, display_title,
				                             visibility, public_log_count)
				VALUES (?, ?, '모순', 'PRIVATE', 1)
				""", userId, extId));
	}
}
