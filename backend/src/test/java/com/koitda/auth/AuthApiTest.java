package com.koitda.auth;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

	@Autowired
	private MockMvc mockMvc;

	private String signupJson(String email, String nickname, boolean privacyAgreed) {
		return """
				{
				  "email": "%s",
				  "password": "password123",
				  "nickname": "%s",
				  "agreements": [
				    {"termsType":"SERVICE","termsVersion":"1.0","agreed":true},
				    {"termsType":"PRIVACY","termsVersion":"1.0","agreed":%s}
				  ]
				}
				""".formatted(email, nickname, privacyAgreed);
	}

	private MvcResult signup(String email, String nickname) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/signup").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupJson(email, nickname, true)))
				.andReturn();
	}

	@Test
	void 회원가입에_성공하면_USER_역할이_부여된다() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupJson("new@koitda.dev", "새회원", true)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nickname").value("새회원"))
				.andExpect(jsonPath("$.roles", contains("USER")));
	}

	@Test
	void 이메일이_중복이면_409() throws Exception {
		signup("dup-email@koitda.dev", "닉A");
		mockMvc.perform(post("/api/v1/auth/signup").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupJson("dup-email@koitda.dev", "닉B", true)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_DUPLICATED"));
	}

	@Test
	void 닉네임이_중복이면_409() throws Exception {
		signup("email-a@koitda.dev", "중복닉");
		mockMvc.perform(post("/api/v1/auth/signup").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupJson("email-b@koitda.dev", "중복닉", true)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("NICKNAME_DUPLICATED"));
	}

	@Test
	void 필수약관에_동의하지_않으면_400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(signupJson("no-terms@koitda.dev", "약관거부", false)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TERMS_REQUIRED"));
	}

	@Test
	void 로그인하면_세션으로_내정보를_조회한다() throws Exception {
		signup("login@koitda.dev", "로그인러");

		MvcResult login = mockMvc.perform(post("/api/v1/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"login@koitda.dev\",\"password\":\"password123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("로그인러"))
				.andReturn();

		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

		mockMvc.perform(get("/api/v1/users/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("login@koitda.dev"))
				.andExpect(jsonPath("$.nickname").value("로그인러"))
				.andExpect(jsonPath("$.roles", contains("USER")));
	}

	@Test
	void 비로그인_상태로_내정보를_조회하면_401() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void 틀린_비밀번호는_401() throws Exception {
		signup("wrongpw@koitda.dev", "비번틀림");
		mockMvc.perform(post("/api/v1/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"wrongpw@koitda.dev\",\"password\":\"WRONGwrong9\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}
}
