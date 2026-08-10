package com.koitda.pattern;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.koitda.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PatternApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	// ---- seed helpers (dev 시더는 test 프로파일에서 실행되지 않으므로 직접 삽입) ----

	private long seedPattern(String title, String craft, String status) {
		long userId = jdbc.queryForObject(
				"INSERT INTO users(nickname) VALUES (?) RETURNING id", Long.class, title + "셀러");
		long sellerId = jdbc.queryForObject(
				"INSERT INTO seller_profile(user_id, brand_name) VALUES (?, ?) RETURNING id",
				Long.class, userId, title + "브랜드");
		long patternId = jdbc.queryForObject("""
				INSERT INTO selling_pattern
				  (seller_id, title, designer_name, craft_type, difficulty,
				   sale_price, regular_price, product_status, published_at)
				VALUES (?, ?, ?, ?, '초급', 10000, 10000, ?, now())
				RETURNING id
				""", Long.class, sellerId, title, "원작자" + title, craft, status);
		long fileId = jdbc.queryForObject("""
				INSERT INTO file_asset(uploader_id, usage_type, storage_key, upload_status)
				VALUES (?, 'PATTERN_IMAGE', ?, 'COMPLETED') RETURNING id
				""", Long.class, userId, "test/" + patternId + ".jpg");
		jdbc.update("INSERT INTO selling_pattern_image(pattern_id, file_id, sort_order, is_thumbnail) "
				+ "VALUES (?, ?, 0, true)", patternId, fileId);
		return patternId;
	}

	private MockHttpSession loginSession(String email) throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"password123","nickname":"%s",
						 "agreements":[{"termsType":"SERVICE","termsVersion":"1.0","agreed":true},
						               {"termsType":"PRIVACY","termsVersion":"1.0","agreed":true}]}
						""".formatted(email, email.split("@")[0])))
				.andExpect(status().isCreated());
		MvcResult login = mockMvc.perform(post("/api/v1/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(email)))
				.andExpect(status().isOk()).andReturn();
		return (MockHttpSession) login.getRequest().getSession(false);
	}

	// ---- tests ----

	@Test
	void 활성_카테고리만_조회된다() {
		jdbc.update("INSERT INTO pattern_category(name, sort_order, is_active) VALUES ('노출카테고리Z', 1, true)");
		jdbc.update("INSERT INTO pattern_category(name, sort_order, is_active) VALUES ('숨김카테고리Z', 2, false)");

		try {
			mockMvc.perform(get("/api/v1/pattern-categories"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$[*].name", hasItem("노출카테고리Z")))
					.andExpect(jsonPath("$[*].name", not(hasItem("숨김카테고리Z"))));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void 목록은_APPROVED_도안만_노출하고_대표이미지를_준다() throws Exception {
		seedPattern("노출도안ABC", "KNIT", "APPROVED");
		seedPattern("초안도안ABC", "KNIT", "DRAFT");

		mockMvc.perform(get("/api/v1/patterns").param("q", "도안ABC"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].title", hasItem("노출도안ABC")))
				.andExpect(jsonPath("$.items[*].title", not(hasItem("초안도안ABC"))));

		mockMvc.perform(get("/api/v1/patterns").param("q", "노출도안ABC"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].thumbnailKey").isNotEmpty())
				.andExpect(jsonPath("$.items[0].craftType").value("KNIT"))
				.andExpect(jsonPath("$.items[0].wished").value(false));
	}

	@Test
	void 위시_등록_해제와_카운트가_동작한다() throws Exception {
		long patternId = seedPattern("위시대상도안", "CROCHET", "APPROVED");
		MockHttpSession session = loginSession("wish-user@koitda.dev");

		// 등록
		mockMvc.perform(post("/api/v1/patterns/" + patternId + "/wish").with(csrf()).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.wished").value(true))
				.andExpect(jsonPath("$.wishCount").value(1));

		// 재등록 → 멱등(카운트 그대로)
		mockMvc.perform(post("/api/v1/patterns/" + patternId + "/wish").with(csrf()).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.wishCount").value(1));

		// 내 위시 목록에 포함
		mockMvc.perform(get("/api/v1/users/me/wishes").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].title", hasItem("위시대상도안")));

		// 해제
		mockMvc.perform(delete("/api/v1/patterns/" + patternId + "/wish").with(csrf()).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.wished").value(false))
				.andExpect(jsonPath("$.wishCount").value(0));
	}

	@Test
	void 비로그인_위시는_401() throws Exception {
		mockMvc.perform(post("/api/v1/patterns/1/wish").with(csrf()))
				.andExpect(status().isUnauthorized());
	}
}
