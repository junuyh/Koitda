package com.koitda.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.koitda.auth.kakao.KakaoOAuthClient;
import com.koitda.auth.kakao.KakaoOAuthClient.KakaoProfile;
import com.koitda.common.error.ApiException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 카카오 프로필 파싱 단위 테스트. 이메일 권한이 없어(닉네임·프로필만 동의) 이메일이 빠진 응답,
 * 닉네임이 레거시 properties 에 오는 응답에서도 회원 식별값을 안전히 뽑는지 확인한다.
 */
class KakaoProfileParseTest {

	private final ObjectMapper mapper = new ObjectMapper();

	private JsonNode json(String s) {
		return mapper.readTree(s);
	}

	@Test
	void 이메일_권한이_없어도_닉네임과_회원번호를_파싱한다() {
		// 동의 항목: 닉네임/프로필만. kakao_account 에 email 없음.
		JsonNode body = json("""
				{"id": 4200001234,
				 "kakao_account": {"profile": {"nickname": "코잇러길동"}}}
				""");
		KakaoProfile p = KakaoOAuthClient.parseProfile(body);
		assertEquals("4200001234", p.providerUid());
		assertEquals("코잇러길동", p.nickname());
		assertNull(p.email(), "이메일 권한이 없으면 email 은 null 이어야 한다");
	}

	@Test
	void 닉네임이_레거시_properties에_와도_파싱한다() {
		JsonNode body = json("""
				{"id": 55, "properties": {"nickname": "레거시닉"}}
				""");
		KakaoProfile p = KakaoOAuthClient.parseProfile(body);
		assertEquals("55", p.providerUid());
		assertEquals("레거시닉", p.nickname());
		assertNull(p.email());
	}

	@Test
	void 이메일이_동의되면_함께_파싱한다() {
		JsonNode body = json("""
				{"id": 77, "kakao_account": {"email": "k@example.com", "profile": {"nickname": "닉"}}}
				""");
		KakaoProfile p = KakaoOAuthClient.parseProfile(body);
		assertEquals("k@example.com", p.email());
		assertEquals("닉", p.nickname());
	}

	@Test
	void 회원번호가_없으면_예외() {
		JsonNode body = json("{\"kakao_account\": {}}");
		assertThrows(ApiException.class, () -> KakaoOAuthClient.parseProfile(body));
	}
}
