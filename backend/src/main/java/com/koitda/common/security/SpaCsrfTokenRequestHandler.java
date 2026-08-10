package com.koitda.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/**
 * SPA + CookieCsrfTokenRepository 표준 처리기 (Spring Security 공식 SPA 패턴).
 * - 렌더링(쿠키에 심는 값)은 XOR 로 BREACH 공격을 방어한다.
 * - 검증 시, 클라이언트가 쿠키의 원시 토큰을 헤더로 보내면 평문으로 해석한다.
 *   (헤더가 없고 폼 파라미터로 온 경우엔 XOR 로 해석)
 */
final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

	private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
	private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
		this.xor.handle(request, response, csrfToken);
	}

	@Override
	public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
		boolean fromHeader = StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()));
		return (fromHeader ? this.plain : this.xor).resolveCsrfTokenValue(request, csrfToken);
	}
}
