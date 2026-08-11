package com.koitda.order;

import static org.hamcrest.Matchers.hasItem;
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

/** ⑧ 데모 주문·결제완료·구매도안·재구매 차단 검증. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OrderApiTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	private long seedApprovedPattern(String title) {
		long sellerUser = jdbc.queryForObject("INSERT INTO users(nickname) VALUES (?) RETURNING id",
				Long.class, title + "셀러");
		long sellerId = jdbc.queryForObject("INSERT INTO seller_profile(user_id, brand_name) VALUES (?, ?) RETURNING id",
				Long.class, sellerUser, title + "브랜드");
		return jdbc.queryForObject("""
				INSERT INTO selling_pattern(seller_id, title, craft_type, sale_price, product_status, published_at)
				VALUES (?, ?, 'KNIT', 12000, 'APPROVED', now()) RETURNING id
				""", Long.class, sellerId, title);
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
	void 주문_결제완료로_구매도안이_생기고_재구매는_차단된다() throws Exception {
		long patternId = seedApprovedPattern("유료가디건");
		MockHttpSession s = loginSession("buyer@koitda.dev");

		// 구매 전: 구매 가능
		mockMvc.perform(get("/api/v1/patterns/" + patternId + "/purchasability").session(s))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.canPurchase").value(true));

		// 주문 생성
		MvcResult ordered = mockMvc.perform(post("/api/v1/orders").with(csrf()).session(s)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"patternId\":" + patternId + ",\"agreed\":true}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("CREATED"))
				.andExpect(jsonPath("$.paymentAmount").value(12000))
				.andReturn();
		long orderId = ((Number) JsonPath.read(ordered.getResponse().getContentAsString(), "$.id")).longValue();

		// 데모 결제 완료 → PAID + 구매 도안
		mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments/complete").with(csrf()).session(s))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orderStatus").value("PAID"))
				.andExpect(jsonPath("$.patternId").value((int) patternId));

		// 구매 도안 목록에 포함
		mockMvc.perform(get("/api/v1/users/me/pattern-library").session(s))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].patternId", hasItem((int) patternId)));

		// 구매 후: 구매 불가
		mockMvc.perform(get("/api/v1/patterns/" + patternId + "/purchasability").session(s))
				.andExpect(jsonPath("$.canPurchase").value(false));

		// 재구매 시도 → 409 ALREADY_OWNED
		mockMvc.perform(post("/api/v1/orders").with(csrf()).session(s)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"patternId\":" + patternId + ",\"agreed\":true}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ALREADY_OWNED"));
	}

	@Test
	void 비로그인_주문은_401() throws Exception {
		mockMvc.perform(post("/api/v1/orders").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"patternId\":1,\"agreed\":true}"))
				.andExpect(status().isUnauthorized());
	}
}
