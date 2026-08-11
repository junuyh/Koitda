package com.koitda.social.service;

import java.util.List;
import java.util.Locale;

/**
 * 댓글 금칙어 검사(SOCIAL-003). 지금은 코드 상수 목록으로 검사한다.
 * ADMIN-003(금칙어 추가·수정·비활성화 + 이력)은 이후 슬라이스에서 테이블·관리 UI 로 옮긴다.
 */
public final class BannedWords {

	private BannedWords() {
	}

	private static final List<String> WORDS = List.of("바보", "멍청이", "욕설", "비속어", "fuck", "shit");

	/** 입력에서 발견된 금칙어를 반환한다(없으면 빈 목록). */
	public static List<String> findMatches(String content) {
		if (content == null || content.isBlank()) {
			return List.of();
		}
		String lower = content.toLowerCase(Locale.ROOT);
		return WORDS.stream().filter(w -> lower.contains(w.toLowerCase(Locale.ROOT))).toList();
	}
}
