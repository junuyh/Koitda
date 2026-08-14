package com.koitda.auth;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 검증 없이(소셜 로그인 등) 이미 신원이 확인된 회원의 세션을 연다.
 * AuthController 의 폼 로그인과 동일하게 SecurityContext 를 세션에 저장한다.
 */
@Component
public class SessionLoginSupport {

	private final SecurityContextRepository securityContextRepository;

	public SessionLoginSupport(SecurityContextRepository securityContextRepository) {
		this.securityContextRepository = securityContextRepository;
	}

	public void establishSession(User user, HttpServletRequest request, HttpServletResponse response) {
		CustomUserDetails principal = new CustomUserDetails(user);
		UsernamePasswordAuthenticationToken authentication =
				UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);
	}
}
