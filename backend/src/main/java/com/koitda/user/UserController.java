package com.koitda.user;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.common.security.CustomUserDetails;
import com.koitda.user.domain.RoleType;
import com.koitda.user.domain.User;
import com.koitda.user.dto.MeResponse;
import com.koitda.user.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
