package com.koitda.project;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
class ProjectApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	private long seedApprovedPattern(String title) {
		long userId = jdbc.queryForObject("INSERT INTO users(nickname) VALUES (?) RETURNING id",
				Long.class, title + "셀러");
		long sellerId = jdbc.queryForObject(
				"INSERT INTO seller_profile(user_id, brand_name) VALUES (?, ?) RETURNING id",
				Long.class, userId, title + "브랜드");
		return jdbc.queryForObject("""
				INSERT INTO selling_pattern
				  (seller_id, title, designer_name, craft_type, difficulty, sale_price, regular_price,
				   product_status, published_at, gauge_info, size_info)
				VALUES (?, ?, ?, 'KNIT', '중급', 18000, 18000, 'APPROVED', now(),
				  '{"stitches":22,"rows":30,"needleSizeMm":4.5}'::jsonb,
				  '{"sizes":[{"label":"M","castOnStitches":148,"measurements":{"chestCm":106}}]}'::jsonb)
				RETURNING id
				""", Long.class, sellerId, title, "원작자");
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

	@Test
	void 외부도안_니팅로그를_생성하고_상세로_재료를_확인한다() throws Exception {
		MockHttpSession session = loginSession("proj-ext@koitda.dev");

		MvcResult created = mockMvc.perform(post("/api/v1/projects").with(csrf()).session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"connectionType":"EXTERNAL",
						 "externalPattern":{"title":"손뜨개 목도리","creatorName":"엄마"},
						 "yarns":[{"brand":"A","yarnName":"메리노","color":"그레이","amount":"3","unit":"볼"}],
						 "gauges":[{"stitches":24,"rows":32,"measuredStage":"SWATCH"}]}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PLANNED"))
				.andExpect(jsonPath("$.visibility").value("PRIVATE"))
				.andExpect(jsonPath("$.displayTitle").isNotEmpty())
				.andReturn();

		Number idNum = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
		long projectId = idNum.longValue();

		mockMvc.perform(get("/api/v1/projects/" + projectId).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.patternType").value("EXTERNAL"))
				.andExpect(jsonPath("$.external.title").value("손뜨개 목도리"))
				.andExpect(jsonPath("$.yarns[0].yarnName").value("메리노"))
				.andExpect(jsonPath("$.gauges[0].stitches").isNumber());
	}

	@Test
	void 판매도안_연결시_원작_게이지가_스냅샷으로_복사된다() throws Exception {
		long patternId = seedApprovedPattern("스냅샷도안");
		MockHttpSession session = loginSession("proj-cat@koitda.dev");

		mockMvc.perform(post("/api/v1/projects").with(csrf()).session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"connectionType\":\"CATALOG\",\"sellingPatternId\":" + patternId + "}"))
				.andExpect(status().isCreated())
				// pattern_snapshot 이 문자열이 아니라 중첩 JSON 이고, 원작 게이지가 복사돼 있어야 한다
				.andExpect(jsonPath("$.patternSnapshot.title").value("스냅샷도안"))
				.andExpect(jsonPath("$.patternSnapshot.gauge.stitches").value(22))
				.andExpect(jsonPath("$.patternSnapshot.sizes[0].castOnStitches").value(148));
	}

	@Test
	void 코잇기는_같은_도안의_니팅로그를_한_타래로_묶는다() throws Exception {
		long patternId = seedApprovedPattern("타래도안");
		MockHttpSession session = loginSession("proj-group@koitda.dev");
		String body = "{\"connectionType\":\"CATALOG\",\"sellingPatternId\":" + patternId + "}";

		for (int i = 0; i < 2; i++) {
			mockMvc.perform(post("/api/v1/projects").with(csrf()).session(session)
					.contentType(MediaType.APPLICATION_JSON).content(body))
					.andExpect(status().isCreated());
		}

		mockMvc.perform(get("/api/v1/projects/grouped-by-pattern").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].patternType").value("CATALOG"))
				.andExpect(jsonPath("$[0].projectCount").value(2));

		// 내 니팅로그 플랫 목록(네이티브 투영 — 날짜 매핑 회귀 방지)
		mockMvc.perform(get("/api/v1/projects/mine").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].createdAt").isNotEmpty());
	}

	@Test
	void 비로그인_생성은_401() throws Exception {
		mockMvc.perform(post("/api/v1/projects").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"connectionType\":\"EXTERNAL\",\"externalPattern\":{\"title\":\"x\"}}"))
				.andExpect(status().isUnauthorized());
	}
}
