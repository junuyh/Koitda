package com.koitda.review;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

/**
 * ⑪ 리뷰→포인트(REVIEW-001~008, POINT-001~003).
 * 구매→리뷰 작성→포인트 적립→삭제→회수→재작성의 핵심 순환과 불변식을 검증한다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ReviewPointTest {

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

	/** APPROVED 도안 하나를 직접 심는다(판매자 등록 흐름은 다른 테스트가 검증). */
	private long seedApprovedPattern(String title) {
		String brand = "리뷰공방_" + title; // users.nickname 은 UNIQUE — 테스트마다 다른 값
		Long sellerUserId = jdbc.queryForObject(
				"INSERT INTO users(nickname) VALUES (?) RETURNING id", Long.class, brand);
		Long sellerId = jdbc.queryForObject(
				"INSERT INTO seller_profile(user_id, brand_name) VALUES (?, ?) RETURNING id",
				Long.class, sellerUserId, brand);
		return jdbc.queryForObject("""
				INSERT INTO selling_pattern
				  (seller_id, title, craft_type, regular_price, sale_price, product_status, published_at)
				VALUES (?, ?, 'KNIT', 10000, 10000, 'APPROVED', now())
				RETURNING id
				""", Long.class, sellerId, title);
	}

	/** 주문→데모 결제 완료로 구매 이력(pattern_library)을 만든다. */
	private void purchase(MockHttpSession session, long patternId) throws Exception {
		MvcResult ordered = mockMvc.perform(post("/api/v1/orders").with(csrf()).session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"patternId\":%d,\"agreed\":true}".formatted(patternId)))
				.andExpect(status().isCreated()).andReturn();
		long orderId = ((Number) JsonPath.read(ordered.getResponse().getContentAsString(), "$.id")).longValue();
		mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments/complete").with(csrf()).session(session))
				.andExpect(status().isOk());
	}

	@Test
	void 구매자가_리뷰를_쓰면_포인트가_적립되고_삭제하면_회수된다() throws Exception {
		signup("rp-reviewer@koitda.dev");
		MockHttpSession user = login("rp-reviewer@koitda.dev");
		long patternId = seedApprovedPattern("리뷰용 스웨터");
		purchase(user, patternId);

		// 리뷰 작성 → 500P 적립
		MvcResult created = mockMvc.perform(post("/api/v1/patterns/" + patternId + "/reviews").with(csrf()).session(user)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"좋아요\",\"contentText\":\"핏이 예뻐요\",\"visibility\":\"PUBLIC\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.earnedPoint").value(500))
				.andExpect(jsonPath("$.pointBalance").value(500))
				.andReturn();
		long reviewId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.reviewId")).longValue();

		// 목록에 노출 + 내 리뷰 식별 + 구매 표시
		mockMvc.perform(get("/api/v1/patterns/" + patternId + "/reviews").session(user))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.purchased").value(true))
				.andExpect(jsonPath("$.myReviewId").value((int) reviewId))
				.andExpect(jsonPath("$.items[0].title").value("좋아요"));

		// 포인트 내역 — 잔액 = 거래합계
		mockMvc.perform(get("/api/v1/users/me/point-transactions").session(user))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.balance").value(500))
				.andExpect(jsonPath("$.transactions[0].txType").value("EARN"))
				.andExpect(jsonPath("$.transactions[0].amount").value(500));

		// 중복 작성 차단(도안당 1개)
		mockMvc.perform(post("/api/v1/patterns/" + patternId + "/reviews").with(csrf()).session(user)
				.contentType(MediaType.APPLICATION_JSON).content("{\"contentText\":\"또 씀\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("REVIEW_ALREADY_EXISTS"));

		// 삭제 → 500P 회수, 잔액 0
		mockMvc.perform(delete("/api/v1/reviews/" + reviewId).with(csrf()).session(user))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.revokedPoint").value(500))
				.andExpect(jsonPath("$.pointBalance").value(0));

		// 잔액 캐시와 거래합계가 일치하고 음수가 아님
		Integer balance = jdbc.queryForObject("SELECT point_balance FROM users WHERE email = ?",
				Integer.class, "rp-reviewer@koitda.dev");
		Integer sum = jdbc.queryForObject("""
				SELECT COALESCE(SUM(amount),0) FROM point_transaction pt
				JOIN users u ON u.id = pt.user_id WHERE u.email = ?
				""", Integer.class, "rp-reviewer@koitda.dev");
		assert balance != null && balance == 0 : "잔액이 0이 아님: " + balance;
		assert sum != null && sum.equals(balance) : "잔액≠거래합계: " + balance + " vs " + sum;

		// 삭제 후 재작성 가능(부분 유니크가 활성 리뷰만 잠금)
		mockMvc.perform(post("/api/v1/patterns/" + patternId + "/reviews").with(csrf()).session(user)
				.contentType(MediaType.APPLICATION_JSON).content("{\"contentText\":\"다시 씀\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	void 미구매자는_리뷰를_쓸_수_없다_403() throws Exception {
		signup("rp-buyer@koitda.dev");
		MockHttpSession buyer = login("rp-buyer@koitda.dev");
		long patternId = seedApprovedPattern("미구매 테스트");

		signup("nonrp-buyer@koitda.dev");
		MockHttpSession nonbuyer = login("nonrp-buyer@koitda.dev");
		mockMvc.perform(post("/api/v1/patterns/" + patternId + "/reviews").with(csrf()).session(nonbuyer)
				.contentType(MediaType.APPLICATION_JSON).content("{\"contentText\":\"살짝 써볼게요\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_PURCHASED"));
	}
}
