package com.koitda.auth;

import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SPA 가 로그인 전에 CSRF 토큰을 받아 XSRF-TOKEN 쿠키를 초기화하기 위한 엔드포인트.
 * CsrfToken 을 파라미터로 받는 것만으로 토큰이 결정되고 쿠키가 설정된다.
 */
@RestController
@RequestMapping("/api/v1/csrf")
public class CsrfController {

	@GetMapping
	public Map<String, String> csrf(CsrfToken token) {
		return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
	}
}
