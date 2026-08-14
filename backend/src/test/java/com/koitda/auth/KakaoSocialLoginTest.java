package com.koitda.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.koitda.TestcontainersConfiguration;
import com.koitda.auth.kakao.KakaoOAuthClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 카카오 소셜 로그인. 실제 카카오 API 대신 KakaoOAuthClient 를 고정 프로필 Fake 로 대체해
 * 콜백 → 개인정보 동의 → 계정 생성 → 세션, 그리고 재로그인(기존 연결) 흐름을 검증한다.
 */
@Import({ TestcontainersConfiguration.class, KakaoSocialLoginTest.FakeKakaoConfig.class })
@SpringBootTest
@AutoConfigureMockMvc
class KakaoSocialLoginTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	private static final String STATE_ATTR = "KAKAO_OAUTH_STATE";

	/** 고정 카카오 프로필을 돌려주는 Fake — 네트워크 없이 흐름만 검증. */
	@TestConfiguration
	static class FakeKakaoConfig {
		@Bean
		@Primary
		KakaoOAuthClient fakeKakaoClient() {
			return new KakaoOAuthClient("test-client", "", "http://localhost:3000/auth/kakao/callback",
					new tools.jackson.databind.ObjectMapper()) {
				@Override
				public boolean isConfigured() {
					return true;
				}

				@Override
				public String authorizeUrl(String state) {
					return "https://kauth.kakao.com/oauth/authorize?state=" + state;
				}

				@Override
				public String exchangeToken(String code) {
					return "access-" + code;
				}

				@Override
				public KakaoProfile fetchProfile(String accessToken) {
					return new KakaoProfile("kakao-uid-777", "kko@example.com", "카카오길동");
				}
			};
		}
	}

	/** GET authorize-url 로 세션에 state 를 심고, 그 state 를 돌려준다. */
	private String startAndGetState(MockHttpSession session) throws Exception {
		mockMvc.perform(get("/api/v1/auth/kakao/authorize-url").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authorizeUrl").exists());
		return (String) session.getAttribute(STATE_ATTR);
	}

	@Test
	void 신규_카카오유저는_동의후_계정이_생기고_재로그인은_바로_로그인된다() throws Exception {
		// 1) 인가 URL → state 확보
		MockHttpSession session = new MockHttpSession();
		String state = startAndGetState(session);
		org.junit.jupiter.api.Assertions.assertNotNull(state);

		// 2) 콜백: 신규 → CONSENT_REQUIRED + ticket + 추천 닉네임
		MvcResult cb = mockMvc.perform(post("/api/v1/auth/kakao/callback").with(csrf()).session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"abc\",\"state\":\"%s\"}".formatted(state)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONSENT_REQUIRED"))
				.andExpect(jsonPath("$.suggestedNickname").value("카카오길동"))
				.andExpect(jsonPath("$.ticket").exists())
				.andReturn();
		String ticket = JsonPath.read(cb.getResponse().getContentAsString(), "$.ticket");

		// 3) 동의 완료 → 계정 생성 + 로그인
		mockMvc.perform(post("/api/v1/auth/kakao/complete").with(csrf()).session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"ticket":"%s","nickname":"카카오길동",
						 "agreements":[{"termsType":"SERVICE","termsVersion":"1.0","agreed":true},
						               {"termsType":"PRIVACY","termsVersion":"1.0","agreed":true}]}
						""".formatted(ticket)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("LOGGED_IN"))
				.andExpect(jsonPath("$.user.nickname").value("카카오길동"));

		// DB: 회원·소셜연결·비밀번호 없음 확인
		Long userId = jdbc.queryForObject(
				"SELECT id FROM users WHERE nickname = '카카오길동'", Long.class);
		Long links = jdbc.queryForObject(
				"SELECT count(*) FROM user_social_login WHERE user_id = ? AND provider='KAKAO' AND provider_uid='kakao-uid-777'",
				Long.class, userId);
		org.junit.jupiter.api.Assertions.assertEquals(1L, links);
		String pw = jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class, userId);
		org.junit.jupiter.api.Assertions.assertNull(pw);

		// 4) 세션으로 내 정보 조회 → 로그인 상태
		mockMvc.perform(get("/api/v1/users/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nickname").value("카카오길동"));

		// 5) 재로그인: 새 세션으로 동일 프로필 콜백 → 동의 없이 바로 LOGGED_IN
		MockHttpSession session2 = new MockHttpSession();
		String state2 = startAndGetState(session2);
		mockMvc.perform(post("/api/v1/auth/kakao/callback").with(csrf()).session(session2)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"def\",\"state\":\"%s\"}".formatted(state2)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("LOGGED_IN"))
				.andExpect(jsonPath("$.user.nickname").value("카카오길동"));
	}

	@Test
	void state가_틀리면_콜백은_거부된다() throws Exception {
		MockHttpSession session = new MockHttpSession();
		startAndGetState(session);
		mockMvc.perform(post("/api/v1/auth/kakao/callback").with(csrf()).session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"abc\",\"state\":\"위조된-state\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void 필수약관_미동의면_계정생성_거부() throws Exception {
		MockHttpSession session = new MockHttpSession();
		String state = startAndGetState(session);
		MvcResult cb = mockMvc.perform(post("/api/v1/auth/kakao/callback").with(csrf()).session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"abc\",\"state\":\"%s\"}".formatted(state)))
				.andExpect(status().isOk()).andReturn();
		String ticket = JsonPath.read(cb.getResponse().getContentAsString(), "$.ticket");

		// PRIVACY 미동의
		mockMvc.perform(post("/api/v1/auth/kakao/complete").with(csrf()).session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"ticket":"%s","nickname":"동의안함",
						 "agreements":[{"termsType":"SERVICE","termsVersion":"1.0","agreed":true},
						               {"termsType":"PRIVACY","termsVersion":"1.0","agreed":false}]}
						""".formatted(ticket)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TERMS_REQUIRED"));
	}
}
