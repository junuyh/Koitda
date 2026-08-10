package com.koitda.auth;

import com.koitda.auth.dto.SignupRequest;
import com.koitda.auth.dto.TermsAgreementRequest;
import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.user.domain.TermsAgreement;
import com.koitda.user.domain.TermsType;
import com.koitda.user.domain.User;
import com.koitda.user.repository.TermsAgreementRepository;
import com.koitda.user.repository.UserRepository;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	/** 가입 시 반드시 동의해야 하는 약관. 마케팅은 선택이다. */
	private static final Set<TermsType> REQUIRED_TERMS = EnumSet.of(TermsType.SERVICE, TermsType.PRIVACY);

	private final UserRepository userRepository;
	private final TermsAgreementRepository termsAgreementRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, TermsAgreementRepository termsAgreementRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.termsAgreementRepository = termsAgreementRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public User signup(SignupRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new ApiException(ErrorCode.EMAIL_DUPLICATED, "이미 사용 중인 이메일입니다.");
		}
		if (userRepository.existsByNickname(request.nickname())) {
			throw new ApiException(ErrorCode.NICKNAME_DUPLICATED, "이미 사용 중인 닉네임입니다.");
		}
		verifyRequiredTerms(request);

		User user = User.createMember(
				request.email(),
				passwordEncoder.encode(request.password()),
				request.nickname());
		userRepository.save(user);

		for (TermsAgreementRequest terms : request.agreements()) {
			termsAgreementRepository.save(new TermsAgreement(
					user.getId(), terms.termsType(), terms.termsVersion(), terms.agreed()));
		}
		return user;
	}

	private void verifyRequiredTerms(SignupRequest request) {
		Set<TermsType> agreed = EnumSet.noneOf(TermsType.class);
		for (TermsAgreementRequest terms : request.agreements()) {
			if (terms.agreed()) {
				agreed.add(terms.termsType());
			}
		}
		if (!agreed.containsAll(REQUIRED_TERMS)) {
			throw new ApiException(ErrorCode.TERMS_REQUIRED, "필수 약관에 동의해야 가입할 수 있습니다.");
		}
	}
}
