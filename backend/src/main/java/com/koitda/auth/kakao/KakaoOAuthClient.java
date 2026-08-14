package com.koitda.auth.kakao;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;

/**
 * 카카오 OAuth2 인가 코드 흐름 클라이언트. Spring Security OAuth2 client(리다이렉트 로그인) 대신
 * 세션 기반 커스텀 인증에 맞춰 토큰 교환·프로필 조회만 직접 호출한다.
 * 키는 환경변수(KAKAO_CLIENT_ID 등)로만 주입 — 저장소·응답에 노출하지 않는다.
 */
@Component
public class KakaoOAuthClient {

	private static final String AUTH_URL = "https://kauth.kakao.com/oauth/authorize";
	private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
	private static final String PROFILE_URL = "https://kapi.kakao.com/v2/user/me";

	private final String clientId;
	private final String clientSecret;
	private final String redirectUri;
	private final RestClient restClient = RestClient.create();

	public KakaoOAuthClient(
			@Value("${kakao.client-id:}") String clientId,
			@Value("${kakao.client-secret:}") String clientSecret,
			@Value("${kakao.redirect-uri:http://localhost:3000/auth/kakao/callback}") String redirectUri) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.redirectUri = redirectUri;
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
		JsonNode body;
		try {
			body = restClient.post().uri(TOKEN_URL)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(JsonNode.class);
		} catch (Exception e) {
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "카카오 인증에 실패했습니다.");
		}
		if (body == null || body.get("access_token") == null) {
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "카카오 토큰을 받지 못했습니다.");
		}
		return body.get("access_token").asString();
	}

	/** 액세스 토큰 → 카카오 프로필(회원번호·이메일·닉네임). */
	public KakaoProfile fetchProfile(String accessToken) {
		requireConfigured();
		JsonNode body;
		try {
			body = restClient.get().uri(PROFILE_URL)
					.header("Authorization", "Bearer " + accessToken)
					.retrieve()
					.body(JsonNode.class);
		} catch (Exception e) {
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "카카오 프로필 조회에 실패했습니다.");
		}
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
