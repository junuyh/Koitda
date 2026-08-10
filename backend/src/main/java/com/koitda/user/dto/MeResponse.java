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
		List<String> roles) {

	public static MeResponse from(User user) {
		List<String> roles = user.getRoles().stream().map(RoleType::name).sorted().toList();
		return new MeResponse(user.getId(), user.getEmail(), user.getNickname(),
				user.getIntro(), user.getPointBalance(), roles);
	}
}
