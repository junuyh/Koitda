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

	public UserController(UserRepository userRepository) {
		this.userRepository = userRepository;
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
