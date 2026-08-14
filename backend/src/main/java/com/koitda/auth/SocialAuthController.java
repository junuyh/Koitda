package com.koitda.auth;

import com.koitda.auth.SocialAuthService.SocialLoginResult;
import com.koitda.auth.dto.AuthUserResponse;
import com.koitda.auth.dto.TermsAgreementRequest;
import com.koitda.auth.kakao.KakaoOAuthClient;
import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.user.domain.User;
import com.koitda.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 카카오 소셜 로그인. 세션 기반 인증에 맞춘 수동 OAuth2 인가 코드 흐름.
 *  1) authorize-url: state 발급(세션 저장) + 카카오 인가 URL 반환
 *  2) callback: code+state 검증 → 기존 연결이면 로그인, 신규면 개인정보 동의 필요(ticket)
 *  3) complete: 동의 후 계정 생성 + 로그인
 */
@RestController
@RequestMapping("/api/v1/auth/kakao")
public class SocialAuthController {

	private static final String STATE_ATTR = "KAKAO_OAUTH_STATE";

	private final SocialAuthService socialAuthService;
	private final KakaoOAuthClient kakaoClient;
	private final SessionLoginSupport sessionLoginSupport;
	private final UserRepository userRepository;

	public SocialAuthController(SocialAuthService socialAuthService, KakaoOAuthClient kakaoClient,
			SessionLoginSupport sessionLoginSupport, UserRepository userRepository) {
		this.socialAuthService = socialAuthService;
		this.kakaoClient = kakaoClient;
		this.sessionLoginSupport = sessionLoginSupport;
		this.userRepository = userRepository;
	}

	public record AuthorizeUrlResponse(String authorizeUrl) {
	}

	public record KakaoCallbackRequest(String code, String state) {
	}

	public record KakaoCallbackResponse(String status, AuthUserResponse user,
			String ticket, String suggestedNickname, String email) {
	}

	public record CompleteRequest(String ticket, String nickname, List<TermsAgreementRequest> agreements) {
	}

	/** 인가 URL 발급. state 를 세션에 저장해 콜백에서 위조를 막는다. */
	@GetMapping("/authorize-url")
	public AuthorizeUrlResponse authorizeUrl(HttpServletRequest request) {
		String state = UUID.randomUUID().toString();
		HttpSession session = request.getSession(true);
		session.setAttribute(STATE_ATTR, state);
		return new AuthorizeUrlResponse(socialAuthService.kakaoAuthorizeUrl(state));
	}

	/** 카카오 콜백: 코드 교환 → 로그인 또는 동의 필요. */
	@PostMapping("/callback")
	public KakaoCallbackResponse callback(@RequestBody KakaoCallbackRequest req,
			HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		Object saved = session != null ? session.getAttribute(STATE_ATTR) : null;
		if (saved == null || req.state() == null || !saved.equals(req.state())) {
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "로그인 요청이 유효하지 않습니다. 다시 시도해주세요.");
		}
		session.removeAttribute(STATE_ATTR);

		SocialLoginResult result = socialAuthService.handleKakaoCallback(req.code());
		if ("LOGGED_IN".equals(result.status())) {
			User user = userRepository.findById(result.userId())
					.orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, "회원을 찾을 수 없습니다."));
			sessionLoginSupport.establishSession(user, request, response);
			return new KakaoCallbackResponse("LOGGED_IN", AuthUserResponse.from(user), null, null, null);
		}
		return new KakaoCallbackResponse("CONSENT_REQUIRED", null,
				result.ticket(), result.suggestedNickname(), result.email());
	}

	/** 개인정보 동의 후 신규 소셜 계정 생성 + 로그인. */
	@PostMapping("/complete")
	public KakaoCallbackResponse complete(@RequestBody CompleteRequest req,
			HttpServletRequest request, HttpServletResponse response) {
		User user = socialAuthService.completeKakaoSignup(req.ticket(), req.nickname(), req.agreements());
		sessionLoginSupport.establishSession(user, request, response);
		return new KakaoCallbackResponse("LOGGED_IN", AuthUserResponse.from(user), null, null, null);
	}

	/** 카카오 로그인 사용 가능 여부(화면에서 버튼 노출 결정용). */
	@GetMapping("/available")
	public java.util.Map<String, Boolean> available() {
		return java.util.Map.of("available", kakaoClient.isConfigured());
	}
}
