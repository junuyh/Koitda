package com.koitda.seller;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

/** ⑩ 판매자 신청·관리자 승인·역할 부여·민감정보 컬럼 암호화 검증. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SellerApplicationTest {

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

	@Test
	void 판매자_신청을_관리자가_승인하면_SELLER_역할이_부여되고_계좌는_암호화된다() throws Exception {
		signup("apply@koitda.dev");
		MockHttpSession user = login("apply@koitda.dev");

		// 신청 (정산 계좌·사업자번호 포함)
		MvcResult applied = mockMvc.perform(post("/api/v1/seller-applications").with(csrf()).session(user)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"brandName":"라라니트","businessType":"INDIVIDUAL","businessNo":"123-45-67890",
						 "representativeName":"홍길동","settlementBank":"코잇은행",
						 "settlementAccount":"110-123-456789","termsVersion":"1.0"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andReturn();
		long appId = ((Number) JsonPath.read(applied.getResponse().getContentAsString(), "$.id")).longValue();

		// 일반 사용자가 승인 시도 → 403
		mockMvc.perform(post("/api/v1/admin/seller-applications/" + appId + "/approve").with(csrf()).session(user))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

		// 관리자 계정 준비(로그인 전에 ADMIN 역할 부여해야 세션 권한에 반영됨)
		long adminId = signup("admin@koitda.dev");
		jdbc.update("INSERT INTO user_role(user_id, role) VALUES (?, 'ADMIN')", adminId);
		MockHttpSession admin = login("admin@koitda.dev");

		// 관리자 승인
		mockMvc.perform(post("/api/v1/admin/seller-applications/" + appId + "/approve").with(csrf()).session(admin))
				.andExpect(status().isOk());

		// 신청자에게 SELLER 역할이 부여됨(겸직 — USER 유지)
		mockMvc.perform(get("/api/v1/users/me/roles").session(user))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasItem("SELLER")))
				.andExpect(jsonPath("$", hasItem("USER")));

		// 정산 계좌는 평문이 아닌 암호문으로 저장됨
		String storedAccount = jdbc.queryForObject(
				"SELECT settlement_account_enc FROM seller_application WHERE id = ?", String.class, appId);
		assertNotEquals("110-123-456789", storedAccount, "계좌가 평문으로 저장됨");
		assertTrue(storedAccount != null && !storedAccount.isBlank(), "계좌 암호문이 비어 있음");
	}

	@Test
	void 관리자_심사_큐에서_신청이_보이고_정산계좌는_마스킹된다() throws Exception {
		signup("queue-apply@koitda.dev");
		MockHttpSession user = login("queue-apply@koitda.dev");
		mockMvc.perform(post("/api/v1/seller-applications").with(csrf()).session(user)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"brandName":"큐니트","businessType":"INDIVIDUAL","businessNo":"111-22-33333",
						 "representativeName":"김판매","settlementBank":"코잇은행",
						 "settlementAccount":"110-999-000111","termsVersion":"1.0"}
						"""))
				.andExpect(status().isCreated());

		long adminId = signup("queue-admin2@koitda.dev");
		jdbc.update("INSERT INTO user_role(user_id, role) VALUES (?, 'ADMIN')", adminId);
		MockHttpSession admin = login("queue-admin2@koitda.dev");

		// 심사 대기 큐에 신청이 나타나고, 정산계좌는 뒤 4자리만 노출된다(개인·금융정보 최소 노출)
		mockMvc.perform(get("/api/v1/admin/seller-applications?status=PENDING").session(admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.brandName == '큐니트')].settlementAccountMasked").value(hasItem("****0111")))
				.andExpect(jsonPath("$[?(@.brandName == '큐니트')].businessNo").value(hasItem("111-22-33333")));

		// 일반 사용자는 심사 큐 접근 불가(403)
		mockMvc.perform(get("/api/v1/admin/seller-applications?status=PENDING").session(user))
				.andExpect(status().isForbidden());
	}

	@Test
	void 심사중_신청이_있으면_중복_신청은_409() throws Exception {
		signup("dup-apply@koitda.dev");
		MockHttpSession user = login("dup-apply@koitda.dev");
		String body = "{\"brandName\":\"브랜드\",\"termsVersion\":\"1.0\"}";
		mockMvc.perform(post("/api/v1/seller-applications").with(csrf()).session(user)
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/seller-applications").with(csrf()).session(user)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("APPLICATION_ALREADY_PENDING"));
	}
}
