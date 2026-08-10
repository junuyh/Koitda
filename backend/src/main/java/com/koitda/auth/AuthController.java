package com.koitda.auth;

import com.koitda.auth.dto.AuthUserResponse;
import com.koitda.auth.dto.LoginRequest;
import com.koitda.auth.dto.SignupRequest;
import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.common.security.CustomUserDetails;
import com.koitda.user.domain.User;
import com.koitda.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final AuthenticationManager authenticationManager;
	private final SecurityContextRepository securityContextRepository;
	private final UserRepository userRepository;

	public AuthController(AuthService authService, AuthenticationManager authenticationManager,
			SecurityContextRepository securityContextRepository, UserRepository userRepository) {
		this.authService = authService;
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.userRepository = userRepository;
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthUserResponse signup(@Valid @RequestBody SignupRequest request) {
		return AuthUserResponse.from(authService.signup(request));
	}

	@PostMapping("/login")
	public AuthUserResponse login(@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
		}
		catch (AuthenticationException ex) {
			throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "이메일 또는 비밀번호가 올바르지 않습니다.");
		}

		// 인증 성공 → SecurityContext 를 세션에 저장해 이후 요청이 로그인 상태로 인식되게 한다.
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, httpRequest, httpResponse);

		CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
		User user = userRepository.findById(principal.getUserId())
				.orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));
		return AuthUserResponse.from(user);
	}
}
