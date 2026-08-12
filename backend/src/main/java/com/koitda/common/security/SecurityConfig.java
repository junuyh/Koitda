package com.koitda.common.security;

import com.koitda.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// SPA 대비 CSRF: 서버가 XSRF-TOKEN 쿠키를 내리고 클라이언트가 헤더로 되돌려준다.
				// SpaCsrfTokenRequestHandler 로 쿠키의 원시 토큰을 헤더로 받아도 검증되게 한다.
				.csrf(csrf -> csrf
						.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
						.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
				.addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/signup", "/api/v1/auth/login").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/csrf").permitAll()
						.requestMatchers("/actuator/health").permitAll()
						// 카탈로그 조회는 비로그인도 허용(위시·내정보 등 나머지는 인증 필요)
						.requestMatchers(HttpMethod.GET, "/api/v1/patterns", "/api/v1/patterns/*",
								"/api/v1/pattern-categories").permitAll()
						// 공개 리뷰 목록·상세는 비로그인도 허용(작성·불러오기·수정·삭제는 인증 필요)
						.requestMatchers(HttpMethod.GET, "/api/v1/patterns/*/reviews", "/api/v1/reviews/*").permitAll()
						// 댓글 수·내용은 비로그인도 조회 가능(SOCIAL-002). 작성·좋아요·팔로우·신고는 인증 필요
						.requestMatchers(HttpMethod.GET, "/api/v1/comments").permitAll()
						// 공개 니팅로그의 적용 게이지 계산은 비로그인도 조회 가능(GAUGE-014)
						.requestMatchers(HttpMethod.GET, "/api/v1/projects/*/gauge-calculation").permitAll()
						// 파일 서빙(이미지)은 공개, 업로드는 인증 필요
						.requestMatchers(HttpMethod.GET, "/api/v1/files/*").permitAll()
						.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
						// 판매자 도안 등록·관리는 SELLER 역할 필요(seller-applications 는 하이픈이라 여기 안 걸림 → 일반 인증)
						.requestMatchers("/api/v1/seller/**").hasRole("SELLER")
						.anyRequest().authenticated())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				// 폼 로그인 리다이렉트 대신 API 답게 401/403 JSON 을 돌려준다.
				.exceptionHandling(eh -> eh
						.authenticationEntryPoint((request, response, ex) ->
								writeError(response, ErrorCode.UNAUTHENTICATED, "로그인이 필요합니다."))
						.accessDeniedHandler((request, response, ex) ->
								writeError(response, ErrorCode.ACCESS_DENIED, "권한이 없습니다.")))
				.logout(logout -> logout
						.logoutUrl("/api/v1/auth/logout")
						.logoutSuccessHandler((request, response, authentication) ->
								response.setStatus(HttpServletResponse.SC_NO_CONTENT)));
		return http.build();
	}

	private void writeError(HttpServletResponse response, ErrorCode code, String message) throws java.io.IOException {
		response.setStatus(code.status().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		// 필드가 모두 내부 고정값(enum 이름·상수 문자열·UUID)이라 이스케이프 없이 안전하게 조립한다.
		String json = "{\"code\":\"" + code.name() + "\",\"message\":\"" + message
				+ "\",\"fieldErrors\":{},\"traceId\":\"" + UUID.randomUUID() + "\"}";
		response.getWriter().write(json);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	public SecurityContextRepository securityContextRepository() {
		// 로그인 성공 시 SecurityContext 를 HTTP 세션에 저장한다(서버 세션 인증).
		return new HttpSessionSecurityContextRepository();
	}
}
