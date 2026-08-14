package com.koitda.inquiry;

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
 * 문의하기(도안 상세). 공개/비공개 열람 제어, 판매자 답변, 판매자 인박스(미답변=알림)를 검증한다.
 * 요구사항 정의서에 없던 사용자 확정 신규 기능.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class InquiryApiTest {

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

	/** 판매자 계정 + 승인된 도안을 만들어 patternId 를 돌려준다. */
	private long[] seedSellerAndPattern(String sellerEmail, String brand) throws Exception {
		long sellerUserId = signup(sellerEmail);
		Long sellerId = jdbc.queryForObject(
				"INSERT INTO seller_profile(user_id, brand_name) VALUES (?, ?) RETURNING id", Long.class, sellerUserId, brand);
		jdbc.update("INSERT INTO user_role(user_id, role) VALUES (?, 'SELLER')", sellerUserId);
		long patternId = jdbc.queryForObject("""
				INSERT INTO selling_pattern (seller_id, title, craft_type, regular_price, sale_price, product_status, published_at)
				VALUES (?, '문의 테스트 도안', 'KNIT', 8000, 8000, 'APPROVED', now()) RETURNING id
				""", Long.class, sellerId);
		return new long[] { sellerUserId, patternId };
	}

	private long createInquiry(MockHttpSession asker, long patternId, String content, boolean isPrivate) throws Exception {
		MvcResult r = mockMvc.perform(post("/api/v1/patterns/" + patternId + "/inquiries").with(csrf()).session(asker)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"content\":\"%s\",\"isPrivate\":%b}".formatted(content, isPrivate)))
				.andExpect(status().isCreated()).andReturn();
		return ((Number) JsonPath.read(r.getResponse().getContentAsString(), "$.id")).longValue();
	}

	@Test
	void 공개문의는_누구나_보고_비공개문의는_작성자와_판매자만_본다() throws Exception {
		long[] s = seedSellerAndPattern("inq-seller@koitda.dev", "문의공방");
		long patternId = s[1];

		MockHttpSession asker = login(loginFor(signup("inq-asker@koitda.dev"), "inq-asker@koitda.dev"));
		createInquiry(asker, patternId, "공개 질문입니다", false);
		long privateId = createInquiry(asker, patternId, "비공개 질문입니다", true);

		// 제3자(비로그인): 공개는 본문, 비공개는 locked(본문 null)
		mockMvc.perform(get("/api/v1/patterns/" + patternId + "/inquiries"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.canAsk").value(false))
				.andExpect(jsonPath("$.items[?(@.isPrivate == true)].locked").value(org.hamcrest.Matchers.hasItem(true)))
				.andExpect(jsonPath("$.items[?(@.isPrivate == true)].content").value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.nullValue())));

		// 작성자 본인: 비공개도 본문 열람
		mockMvc.perform(get("/api/v1/patterns/" + patternId + "/inquiries").session(asker))
				.andExpect(jsonPath("$.items[?(@.id == " + privateId + ")].locked").value(org.hamcrest.Matchers.hasItem(false)))
				.andExpect(jsonPath("$.items[?(@.id == " + privateId + ")].content").value(org.hamcrest.Matchers.hasItem("비공개 질문입니다")));

		// 판매자: 비공개도 본문 열람 + canAnswer
		MockHttpSession seller = login("inq-seller@koitda.dev");
		mockMvc.perform(get("/api/v1/patterns/" + patternId + "/inquiries").session(seller))
				.andExpect(jsonPath("$.isSeller").value(true))
				.andExpect(jsonPath("$.items[?(@.id == " + privateId + ")].content").value(org.hamcrest.Matchers.hasItem("비공개 질문입니다")));
	}

	@Test
	void 판매자가_답변하면_answered이고_남의도안_판매자는_답변할수없다() throws Exception {
		long[] s = seedSellerAndPattern("inq-seller2@koitda.dev", "문의공방2");
		long patternId = s[1];
		MockHttpSession asker = login(loginFor(signup("inq-asker2@koitda.dev"), "inq-asker2@koitda.dev"));
		long inquiryId = createInquiry(asker, patternId, "언제 발송되나요?", false);

		// 다른 판매자는 이 도안에 답변 불가(403)
		seedSellerAndPattern("other-seller@koitda.dev", "다른공방");
		MockHttpSession other = login("other-seller@koitda.dev");
		mockMvc.perform(post("/api/v1/inquiries/" + inquiryId + "/answer").with(csrf()).session(other)
				.contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"내 도안 아님\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

		// 이 도안 판매자는 답변 가능
		MockHttpSession seller = login("inq-seller2@koitda.dev");
		mockMvc.perform(post("/api/v1/inquiries/" + inquiryId + "/answer").with(csrf()).session(seller)
				.contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"3일 내 발송됩니다\"}"))
				.andExpect(status().isOk());

		// 목록에 답변 반영
		mockMvc.perform(get("/api/v1/patterns/" + patternId + "/inquiries").session(asker))
				.andExpect(jsonPath("$.items[?(@.id == " + inquiryId + ")].answered").value(org.hamcrest.Matchers.hasItem(true)))
				.andExpect(jsonPath("$.items[?(@.id == " + inquiryId + ")].answer").value(org.hamcrest.Matchers.hasItem("3일 내 발송됩니다")));
	}

	@Test
	void 판매자_인박스는_내_도안_문의만_보이고_미답변_카운트를_준다() throws Exception {
		long[] s = seedSellerAndPattern("inbox-seller@koitda.dev", "인박스공방");
		long patternId = s[1];
		MockHttpSession asker = login(loginFor(signup("inbox-asker@koitda.dev"), "inbox-asker@koitda.dev"));
		long q1 = createInquiry(asker, patternId, "질문1", false);
		createInquiry(asker, patternId, "질문2", true);

		MockHttpSession seller = login("inbox-seller@koitda.dev");
		// 미답변 2건
		mockMvc.perform(get("/api/v1/seller/inquiries").session(seller))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unansweredCount").value(2))
				.andExpect(jsonPath("$.items.length()").value(2));

		// 하나 답변 → 미답변 1건, unanswered=true 필터 시 1건
		mockMvc.perform(post("/api/v1/inquiries/" + q1 + "/answer").with(csrf()).session(seller)
				.contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"답변\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/seller/inquiries?unanswered=true").session(seller))
				.andExpect(jsonPath("$.unansweredCount").value(1))
				.andExpect(jsonPath("$.items.length()").value(1));
	}

	@Test
	void 작성자는_문의를_삭제할수있고_목록에서_사라진다() throws Exception {
		long[] s = seedSellerAndPattern("del-seller@koitda.dev", "삭제공방");
		long patternId = s[1];
		MockHttpSession asker = login(loginFor(signup("del-asker@koitda.dev"), "del-asker@koitda.dev"));
		long inquiryId = createInquiry(asker, patternId, "지울 질문", false);

		mockMvc.perform(delete("/api/v1/inquiries/" + inquiryId).with(csrf()).session(asker))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/patterns/" + patternId + "/inquiries"))
				.andExpect(jsonPath("$.items.length()").value(0));
	}

	@Test
	void 비로그인은_문의를_작성할수없다_401() throws Exception {
		long[] s = seedSellerAndPattern("anon-seller@koitda.dev", "익명공방");
		mockMvc.perform(post("/api/v1/patterns/" + s[1] + "/inquiries").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"익명 질문\",\"isPrivate\":false}"))
				.andExpect(status().isUnauthorized());
	}

	/** signup 은 id 를 주므로, 로그인용 이메일을 그대로 반환하는 헬퍼(가독성용). */
	private String loginFor(long ignoredUserId, String email) {
		return email;
	}
}
