package com.koitda.user;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.common.security.CustomUserDetails;
import com.koitda.user.domain.RoleType;
import com.koitda.user.domain.User;
import com.koitda.user.dto.MeResponse;
import com.koitda.user.repository.UserRepository;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

	private final UserRepository userRepository;
	private final EmailVerificationService verificationService;
	private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

	public UserController(UserRepository userRepository, EmailVerificationService verificationService,
			org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.verificationService = verificationService;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping
	public MeResponse me(@AuthenticationPrincipal CustomUserDetails principal) {
		return MeResponse.from(currentUser(principal));
	}

	/** 닉네임 변경(마이페이지 정보 수정). */
	@PatchMapping
	@Transactional
	public MeResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		User user = currentUser(principal);
		user.changeNickname(request.nickname().trim());
		return MeResponse.from(userRepository.save(user));
	}

	public record UpdateProfileRequest(@NotBlank @Size(max = 30) String nickname) {
	}

	/** 비밀번호 변경용 이메일 인증번호 발급(데모: 응답에 코드 노출). */
	@org.springframework.web.bind.annotation.PostMapping("/password/verification")
	public VerificationIssued issuePasswordCode(@AuthenticationPrincipal CustomUserDetails principal) {
		User user = currentUser(principal);
		String code = verificationService.issue(user.getId());
		return new VerificationIssued(user.getEmail(), code);
	}

	/** 비밀번호 변경 — 이메일 인증번호 확인 후 적용. */
	@PatchMapping("/password")
	@Transactional
	public void changePassword(@Valid @RequestBody ChangePasswordRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		User user = currentUser(principal);
		verificationService.verify(user.getId(), request.code());
		user.changePassword(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);
	}

	/** 데모: demoCode 는 실제 운영에서 메일로만 전달하고 응답에서 제거한다. */
	public record VerificationIssued(String email, String demoCode) {
	}

	public record ChangePasswordRequest(@NotBlank String code, @NotBlank @Size(min = 8, max = 72) String newPassword) {
	}

	@GetMapping("/roles")
	public List<String> myRoles(@AuthenticationPrincipal CustomUserDetails principal) {
		return currentUser(principal).getRoles().stream().map(RoleType::name).sorted().toList();
	}

	private User currentUser(CustomUserDetails principal) {
		if (principal == null) {
			throw new ApiException(ErrorCode.UNAUTHENTICATED);
		}
		return userRepository.findById(principal.getUserId())
				.orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
	}
}
