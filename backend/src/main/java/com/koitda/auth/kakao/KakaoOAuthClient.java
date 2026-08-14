package com.koitda.auth.kakao;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

/**
 * 카카오 OAuth2 인가 코드 흐름 클라이언트. Spring Security OAuth2 client(리다이렉트 로그인) 대신
 * 세션 기반 커스텀 인증에 맞춰 토큰 교환·프로필 조회만 직접 호출한다.
 * 키는 환경변수(KAKAO_CLIENT_ID 등)로만 주입 — 저장소·응답에 노출하지 않는다.
 */
@Component
public class KakaoOAuthClient {

	private static final Logger log = LoggerFactory.getLogger(KakaoOAuthClient.class);
	private static final String AUTH_URL = "https://kauth.kakao.com/oauth/authorize";
	private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
	private static final String PROFILE_URL = "https://kapi.kakao.com/v2/user/me";

	private final String clientId;
	private final String clientSecret;
	private final String redirectUri;
	private final RestClient restClient = RestClient.create();
	private final tools.jackson.databind.ObjectMapper objectMapper;

	public KakaoOAuthClient(
			@Value("${kakao.client-id:}") String clientId,
			@Value("${kakao.client-secret:}") String clientSecret,
			@Value("${kakao.redirect-uri:http://localhost:3000/auth/kakao/callback}") String redirectUri,
			tools.jackson.databind.ObjectMapper objectMapper) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.redirectUri = redirectUri;
		this.objectMapper = objectMapper;
	}

	public boolean isConfigured() {
		return clientId != null && !clientId.isBlank();
	}

	/** 카카오 인가 화면 URL. state 는 CSRF 방지용(세션에 저장한 값과 콜백에서 대조). */
	public String authorizeUrl(String state) {
		requireConfigured();
		return UriComponentsBuilder.fromUriString(AUTH_URL)
				.queryParam("response_type", "code")
				.queryParam("client_id", clientId)
				.queryParam("redirect_uri", redirectUri)
				.queryParam("state", state)
				.build(true).toUriString();
	}

	/** 인가 코드 → 액세스 토큰. */
	public String exchangeToken(String code) {
		requireConfigured();
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", clientId);
		form.add("redirect_uri", redirectUri);
		form.add("code", code);
		if (clientSecret != null && !clientSecret.isBlank()) {
			form.add("client_secret", clientSecret);
		}
		String raw;
		try {
			// 컨버터 설정에 의존하지 않도록 문자열로 받아 Jackson 3 으로 직접 파싱한다.
			raw = restClient.post().uri(TOKEN_URL)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(String.class);
		} catch (RestClientResponseException e) {
			// 카카오가 4xx/5xx 로 준 에러 본문(error, error_description)을 로그로 남긴다 — 원인 진단용.
			log.warn("카카오 토큰 교환 실패: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "카카오 인증에 실패했습니다.");
		} catch (Exception e) {
			log.warn("카카오 토큰 교환 중 오류", e);
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "카카오 인증에 실패했습니다.");
		}
		JsonNode body = readTree(raw);
		if (body == null || body.get("access_token") == null) {
			log.warn("카카오 토큰 응답에 access_token 없음: {}", raw);
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "카카오 토큰을 받지 못했습니다.");
		}
		return body.get("access_token").asString();
	}

	/** 액세스 토큰 → 카카오 프로필(회원번호·이메일·닉네임). */
	public KakaoProfile fetchProfile(String accessToken) {
		requireConfigured();
		String raw;
		try {
			raw = restClient.get().uri(PROFILE_URL)
					.header("Authorization", "Bearer " + accessToken)
					.retrieve()
					.body(String.class);
		} catch (RestClientResponseException e) {
			log.warn("카카오 프로필 조회 실패: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "카카오 프로필 조회에 실패했습니다.");
		} catch (Exception e) {
			log.warn("카카오 프로필 조회 중 오류", e);
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "카카오 프로필 조회에 실패했습니다.");
		}
		return parseProfile(readTree(raw));
	}

	private JsonNode readTree(String raw) {
		try {
			return raw == null ? null : objectMapper.readTree(raw);
		} catch (Exception e) {
			log.warn("카카오 응답 JSON 파싱 실패: {}", raw);
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "카카오 응답을 해석하지 못했습니다.");
		}
	}

	/**
	 * 카카오 /v2/user/me 응답 파싱. 동의 항목에 따라 필드가 없을 수 있어 방어적으로 읽는다.
	 *  · 이메일: 비즈앱 심사 전에는 권한이 없어 대개 null — 그대로 null 을 허용한다(가입 가능).
	 *  · 닉네임: 앱 설정에 따라 kakao_account.profile.nickname 또는 레거시 properties.nickname 에 온다.
	 */
	public static KakaoProfile parseProfile(JsonNode body) {
		if (body == null || body.get("id") == null) {
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "카카오 프로필을 받지 못했습니다.");
		}
		String uid = body.get("id").asString();

		JsonNode account = body.get("kakao_account");
		String email = (account != null && account.hasNonNull("email")) ? account.get("email").asString() : null;

		String nickname = null;
		if (account != null && account.get("profile") != null && account.get("profile").hasNonNull("nickname")) {
			nickname = account.get("profile").get("nickname").asString();
		}
		// 레거시/대체 위치: properties.nickname
		if (nickname == null && body.get("properties") != null && body.get("properties").hasNonNull("nickname")) {
			nickname = body.get("properties").get("nickname").asString();
		}
		return new KakaoProfile(uid, email, nickname);
	}

	private void requireConfigured() {
		if (!isConfigured()) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR,
					"카카오 로그인이 설정되지 않았습니다. 관리자에게 문의하세요.");
		}
	}

	/** 카카오에서 받은 최소 프로필. */
	public record KakaoProfile(String providerUid, String email, String nickname) {
	}
}
