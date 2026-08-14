package com.koitda.user.dto;

import com.koitda.user.domain.RoleType;
import com.koitda.user.domain.User;
import java.util.List;

/** 내 정보 응답. Entity 를 그대로 노출하지 않고 필요한 필드만 담는다. */
public record MeResponse(
		Long id,
		String email,
		String nickname,
		String intro,
		int pointBalance,
		List<String> roles,
		boolean hasPassword) {

	public static MeResponse from(User user) {
		List<String> roles = user.getRoles().stream().map(RoleType::name).sorted().toList();
		// 소셜 전용 회원(카카오)은 비밀번호가 없다 — 화면에서 비밀번호 변경을 감춘다.
		boolean hasPassword = user.getPasswordHash() != null && !user.getPasswordHash().isBlank();
		return new MeResponse(user.getId(), user.getEmail(), user.getNickname(),
				user.getIntro(), user.getPointBalance(), roles, hasPassword);
	}
}
