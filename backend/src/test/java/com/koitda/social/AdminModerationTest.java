package com.koitda.social;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.koitda.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** ADMIN-002 신고 관리: 신고 접수 → 관리자 큐 → 숨김(리뷰 즉시 비공개) → 복원. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AdminModerationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	private long signup(String email) throws Exception {
		MvcResult r = mockMvc.perform(post("/api/v1/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"password123","nickname":"%s",
						 "agreements":[{"termsType":"SERVICE","termsVersion":"1.0","agreed":true},
						               {"termsType":"PRIVACY","termsVersion":"1.0","agreed":true}]}
						""".formatted(email, email.split("@")[0]))).andExpect(status().isCreated()).andReturn();
		return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
	}

	private MockHttpSession login(String email) throws Exception {
		MvcResult r = mockMvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(email)))
				.andExpect(status().isOk()).andReturn();
		return (MockHttpSession) r.getRequest().getSession(false);
	}

	private long seedApprovedPattern(String title) {
		String brand = "신고공방_" + title;
		Long su = jdbc.queryForObject("INSERT INTO users(nickname) VALUES (?) RETURNING id", Long.class, brand);
		Long sid = jdbc.queryForObject(
				"INSERT INTO seller_profile(user_id, brand_name) VALUES (?, ?) RETURNING id", Long.class, su, brand);
		return jdbc.queryForObject("""
				INSERT INTO selling_pattern (seller_id, title, craft_type, regular_price, sale_price, product_status, published_at)
				VALUES (?, ?, 'KNIT', 10000, 10000, 'APPROVED', now()) RETURNING id
				""", Long.class, sid, title);
	}

	private void purchase(MockHttpSession s, long patternId) throws Exception {
		MvcResult o = mockMvc.perform(post("/api/v1/orders").with(csrf()).session(s)
				.contentType(MediaType.APPLICATION_JSON).content("{\"patternId\":%d,\"agreed\":true}".formatted(patternId)))
				.andExpect(status().isCreated()).andReturn();
		long orderId = ((Number) JsonPath.read(o.getResponse().getContentAsString(), "$.id")).longValue();
		mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments/complete").with(csrf()).session(s))
				.andExpect(status().isOk());
	}

	@Test
	void 신고된_리뷰를_관리자가_숨기면_공개목록에서_사라지고_복원하면_다시_보인다() throws Exception {
		// 작성자: 구매 후 공개 리뷰 작성
		signup("mod-author@koitda.dev");
		MockHttpSession author = login("mod-author@koitda.dev");
		long patternId = seedApprovedPattern("신고 스웨터");
		purchase(author, patternId);
		MvcResult rv = mockMvc.perform(post("/api/v1/patterns/" + patternId + "/reviews").with(csrf()).session(author)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"문제 리뷰\",\"contentText\":\"부적절 내용\",\"rating\":3,\"visibility\":\"PUBLIC\"}"))
				.andExpect(status().isCreated()).andReturn();
		long reviewId = ((Number) JsonPath.read(rv.getResponse().getContentAsString(), "$.reviewId")).longValue();

		// 다른 사용자가 신고
		signup("mod-reporter@koitda.dev");
		MockHttpSession reporter = login("mod-reporter@koitda.dev");
		mockMvc.perform(post("/api/v1/reports").with(csrf()).session(reporter)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"targetType\":\"REVIEW\",\"targetId\":%d,\"reasonCode\":\"INAPPROPRIATE\"}".formatted(reviewId)))
				.andExpect(status().isCreated());

		// 관리자 계정
		long adminId = signup("mod-admin@koitda.dev");
		jdbc.update("INSERT INTO user_role(user_id, role) VALUES (?, 'ADMIN')", adminId);
		MockHttpSession admin = login("mod-admin@koitda.dev");

		// 신고 큐에 리뷰가 보인다
		MvcResult q = mockMvc.perform(get("/api/v1/admin/reports").session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.targetId == " + reviewId + ")].targetType").value(org.hamcrest.Matchers.hasItem("REVIEW")))
				.andReturn();
		java.util.List<Number> modIds = JsonPath.read(q.getResponse().getContentAsString(),
				"$[?(@.targetId == " + reviewId + ")].moderationId");
		long modId = modIds.get(0).longValue();

		// 숨김 처리
		mockMvc.perform(post("/api/v1/admin/reports/" + modId + "/hide").with(csrf()).session(admin))
				.andExpect(status().isOk());

		// 공개 리뷰 목록에서 사라짐(비로그인 조회)
		mockMvc.perform(get("/api/v1/patterns/" + patternId + "/reviews"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(0));

		// 복원하면 다시 보인다
		mockMvc.perform(post("/api/v1/admin/reports/" + modId + "/restore").with(csrf()).session(admin))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/patterns/" + patternId + "/reviews"))
				.andExpect(jsonPath("$.items.length()").value(1));

		// 비관리자는 신고 큐 접근 불가(403)
		mockMvc.perform(get("/api/v1/admin/reports").session(reporter))
				.andExpect(status().isForbidden());
	}
}
