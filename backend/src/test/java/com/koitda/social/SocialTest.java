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

/** ⑩ 소셜(SOCIAL-001~004, SOCIAL-003 금칙어, REPORT-001). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SocialTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	private long signup(String email) throws Exception {
		MvcResult r = mockMvc.perform(post("/api/v1/auth/signup").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"password123","nickname":"%s",
						 "agreements":[{"termsType":"SERVICE","termsVersion":"1.0","agreed":true},
						               {"termsType":"PRIVACY","termsVersion":"1.0","agreed":true}]}
						""".formatted(email, email.split("@")[0])))
				.andExpect(status().isCreated()).andReturn();
		return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
	}

	private MockHttpSession login(String email) throws Exception {
		MvcResult r = mockMvc.perform(post("/api/v1/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"password123\"}".formatted(email)))
				.andExpect(status().isOk()).andReturn();
		return (MockHttpSession) r.getRequest().getSession(false);
	}

	private long seedApprovedPattern(String title) {
		Long su = jdbc.queryForObject("INSERT INTO users(nickname) VALUES (?) RETURNING id",
				Long.class, "소셜공방_" + title);
		Long sid = jdbc.queryForObject("INSERT INTO seller_profile(user_id, brand_name) VALUES (?, ?) RETURNING id",
				Long.class, su, "소셜공방_" + title);
		return jdbc.queryForObject("""
				INSERT INTO selling_pattern (seller_id, title, craft_type, regular_price, sale_price, product_status, published_at)
				VALUES (?, ?, 'KNIT', 10000, 10000, 'APPROVED', now()) RETURNING id
				""", Long.class, sid, title);
	}

	private long buyAndReview(MockHttpSession s, long patternId) throws Exception {
		MvcResult ord = mockMvc.perform(post("/api/v1/orders").with(csrf()).session(s)
				.contentType(MediaType.APPLICATION_JSON).content("{\"patternId\":%d,\"agreed\":true}".formatted(patternId)))
				.andExpect(status().isCreated()).andReturn();
		long orderId = ((Number) JsonPath.read(ord.getResponse().getContentAsString(), "$.id")).longValue();
		mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments/complete").with(csrf()).session(s))
				.andExpect(status().isOk());
		MvcResult rev = mockMvc.perform(post("/api/v1/patterns/" + patternId + "/reviews").with(csrf()).session(s)
				.contentType(MediaType.APPLICATION_JSON).content("{\"contentText\":\"좋은 도안\"}"))
				.andExpect(status().isCreated()).andReturn();
		return ((Number) JsonPath.read(rev.getResponse().getContentAsString(), "$.reviewId")).longValue();
	}

	@Test
	void 좋아요_토글과_댓글_금칙어_검사가_동작한다() throws Exception {
		signup("sc-author@koitda.dev");
		MockHttpSession author = login("sc-author@koitda.dev");
		long patternId = seedApprovedPattern("소셜 스웨터");
		long reviewId = buyAndReview(author, patternId);

		// 다른 사용자가 좋아요 → 1, 다시 누르면 → 0
		signup("sc-fan@koitda.dev");
		MockHttpSession fan = login("sc-fan@koitda.dev");
		String likeBody = "{\"targetType\":\"REVIEW\",\"targetId\":%d}".formatted(reviewId);
		mockMvc.perform(post("/api/v1/likes").with(csrf()).session(fan)
				.contentType(MediaType.APPLICATION_JSON).content(likeBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked").value(true))
				.andExpect(jsonPath("$.likeCount").value(1));
		mockMvc.perform(post("/api/v1/likes").with(csrf()).session(fan)
				.contentType(MediaType.APPLICATION_JSON).content(likeBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.liked").value(false))
				.andExpect(jsonPath("$.likeCount").value(0));

		// 댓글 등록 → 목록에 반영
		mockMvc.perform(post("/api/v1/comments").with(csrf()).session(fan)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"targetType\":\"REVIEW\",\"targetId\":%d,\"content\":\"저도 떠봤어요!\"}".formatted(reviewId)))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/api/v1/comments?targetType=REVIEW&targetId=" + reviewId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(1))
				.andExpect(jsonPath("$.items[0].content").value("저도 떠봤어요!"));

		// 금칙어 댓글 → 400 BANNED_WORD
		mockMvc.perform(post("/api/v1/comments").with(csrf()).session(fan)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"targetType\":\"REVIEW\",\"targetId\":%d,\"content\":\"이 바보야\"}".formatted(reviewId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BANNED_WORD"));
	}

	@Test
	void 자기_자신_팔로우는_막고_중복_신고는_409() throws Exception {
		long meId = signup("sc-me@koitda.dev");
		MockHttpSession me = login("sc-me@koitda.dev");
		long otherId = signup("sc-other@koitda.dev");

		// 자기 팔로우 → 400
		mockMvc.perform(post("/api/v1/follows/" + meId).with(csrf()).session(me))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("CANNOT_FOLLOW_SELF"));
		// 정상 팔로우 → 목록에 반영
		mockMvc.perform(post("/api/v1/follows/" + otherId).with(csrf()).session(me)).andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/users/me/following").session(me))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].userId").value((int) otherId));

		// 신고 → 1건, 같은 콘텐츠 재신고 → 409
		String rep = "{\"targetType\":\"REVIEW\",\"targetId\":1,\"reasonCode\":\"SPAM\"}";
		mockMvc.perform(post("/api/v1/reports").with(csrf()).session(me)
				.contentType(MediaType.APPLICATION_JSON).content(rep))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.validReportCount").value(1));
		mockMvc.perform(post("/api/v1/reports").with(csrf()).session(me)
				.contentType(MediaType.APPLICATION_JSON).content(rep))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ALREADY_REPORTED"));
	}
}
