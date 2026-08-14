package com.koitda.auth;

import com.koitda.auth.domain.UserSocialLogin;
import com.koitda.auth.dto.TermsAgreementRequest;
import com.koitda.auth.kakao.KakaoOAuthClient;
import com.koitda.auth.kakao.KakaoOAuthClient.KakaoProfile;
import com.koitda.auth.repository.UserSocialLoginRepository;
import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.user.domain.TermsAgreement;
import com.koitda.user.domain.TermsType;
import com.koitda.user.domain.User;
import com.koitda.user.repository.TermsAgreementRepository;
import com.koitda.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카카오 소셜 로그인. 기존 연결이면 바로 로그인, 신규면 개인정보 동의를 받아 계정을 만든다.
 * 신규 프로필은 짧은 수명의 인메모리 티켓(EmailVerificationService 와 동일 관례)에 담아
 * 동의 화면을 거친 뒤에만 계정으로 승격시킨다.
 */
@Service
public class SocialAuthService {

	private static final String KAKAO = "KAKAO";
	private static final Set<TermsType> REQUIRED_TERMS = EnumSet.of(TermsType.SERVICE, TermsType.PRIVACY);

	private record PendingSocial(String provider, String providerUid, String email, String nickname, Instant expiresAt) {
	}

	private final ConcurrentHashMap<String, PendingSocial> pending = new ConcurrentHashMap<>();

	private final KakaoOAuthClient kakaoClient;
	private final UserRepository userRepository;
	private final UserSocialLoginRepository socialLoginRepository;
	private final TermsAgreementRepository termsAgreementRepository;

	public SocialAuthService(KakaoOAuthClient kakaoClient, UserRepository userRepository,
			UserSocialLoginRepository socialLoginRepository, TermsAgreementRepository termsAgreementRepository) {
		this.kakaoClient = kakaoClient;
		this.userRepository = userRepository;
		this.socialLoginRepository = socialLoginRepository;
		this.termsAgreementRepository = termsAgreementRepository;
	}

	/** 인가 화면 URL. state 는 컨트롤러가 세션에 저장한 값. */
	public String kakaoAuthorizeUrl(String state) {
		return kakaoClient.authorizeUrl(state);
	}

	/**
	 * 카카오 콜백 처리(코드 → 프로필). 이미 연결된 회원이면 LOGGED_IN(userId),
	 * 신규면 CONSENT_REQUIRED(ticket) 를 돌려준다.
	 */
	@Transactional
	public SocialLoginResult handleKakaoCallback(String code) {
		String accessToken = kakaoClient.exchangeToken(code);
		KakaoProfile profile = kakaoClient.fetchProfile(accessToken);

		return socialLoginRepository.findByProviderAndProviderUid(KAKAO, profile.providerUid())
				.map(link -> SocialLoginResult.loggedIn(link.getUserId()))
				.orElseGet(() -> {
					String ticket = UUID.randomUUID().toString();
					pending.put(ticket, new PendingSocial(KAKAO, profile.providerUid(), profile.email(),
							profile.nickname(), Instant.now().plus(10, ChronoUnit.MINUTES)));
					return SocialLoginResult.consentRequired(ticket, suggestNickname(profile.nickname()), profile.email());
				});
	}

	/**
	 * 개인정보 동의 후 신규 소셜 계정 생성. 필수 약관 미동의 시 거부한다.
	 * 티켓의 소셜 프로필로 users + user_social_login + terms_agreement 를 함께 만든다.
	 */
	@Transactional
	public User completeKakaoSignup(String ticket, String nickname, List<TermsAgreementRequest> agreements) {
		PendingSocial p = pending.get(ticket);
		if (p == null || p.expiresAt().isBefore(Instant.now())) {
			pending.remove(ticket);
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "인증이 만료되었습니다. 다시 로그인해주세요.");
		}
		verifyRequiredTerms(agreements);

		// 동시 콜백으로 이미 연결됐다면 그 회원으로 로그인(이중 생성 방지).
		var existing = socialLoginRepository.findByProviderAndProviderUid(p.provider(), p.providerUid());
		if (existing.isPresent()) {
			pending.remove(ticket);
			return userRepository.findById(existing.get().getUserId())
					.orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, "회원을 찾을 수 없습니다."));
		}

		String finalNickname = resolveNickname(nickname != null && !nickname.isBlank() ? nickname : p.nickname());
		User user = User.createSocialMember(p.email(), finalNickname);
		userRepository.save(user);
		try {
			socialLoginRepository.saveAndFlush(new UserSocialLogin(user.getId(), p.provider(), p.providerUid()));
		} catch (org.springframework.dao.DataIntegrityViolationException e) {
			// 유니크 충돌(동시 요청) — 이미 만들어진 연결로 로그인.
			pending.remove(ticket);
			var link = socialLoginRepository.findByProviderAndProviderUid(p.provider(), p.providerUid())
					.orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS, "카카오 로그인에 실패했습니다."));
			return userRepository.findById(link.getUserId())
					.orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND, "회원을 찾을 수 없습니다."));
		}
		for (TermsAgreementRequest terms : agreements) {
			termsAgreementRepository.save(new TermsAgreement(
					user.getId(), terms.termsType(), terms.termsVersion(), terms.agreed()));
		}
		pending.remove(ticket);
		return user;
	}

	private void verifyRequiredTerms(List<TermsAgreementRequest> agreements) {
		Set<TermsType> agreed = EnumSet.noneOf(TermsType.class);
		if (agreements != null) {
			for (TermsAgreementRequest terms : agreements) {
				if (terms.agreed()) {
					agreed.add(terms.termsType());
				}
			}
		}
		if (!agreed.containsAll(REQUIRED_TERMS)) {
			throw new ApiException(ErrorCode.TERMS_REQUIRED, "필수 약관에 동의해야 가입할 수 있습니다.");
		}
	}

	/** 화면에 채워줄 추천 닉네임(중복이면 뒤에 숫자). 비면 '코잇러'. */
	private String suggestNickname(String raw) {
		return resolveNickname(raw);
	}

	private String resolveNickname(String raw) {
		String base = (raw == null || raw.isBlank()) ? "코잇러" : raw.trim();
		if (base.length() > 20) {
			base = base.substring(0, 20);
		}
		if (!userRepository.existsByNickname(base)) {
			return base;
		}
		for (int i = 1; i <= 9999; i++) {
			String candidate = base + i;
			if (!userRepository.existsByNickname(candidate)) {
				return candidate;
			}
		}
		return base + UUID.randomUUID().toString().substring(0, 6);
	}

	/** 콜백 결과. status 는 LOGGED_IN(userId 유효) 또는 CONSENT_REQUIRED(ticket 유효). */
	public record SocialLoginResult(String status, Long userId, String ticket,
			String suggestedNickname, String email) {

		static SocialLoginResult loggedIn(Long userId) {
			return new SocialLoginResult("LOGGED_IN", userId, null, null, null);
		}

		static SocialLoginResult consentRequired(String ticket, String suggestedNickname, String email) {
			return new SocialLoginResult("CONSENT_REQUIRED", null, ticket, suggestedNickname, email);
		}
	}
}
