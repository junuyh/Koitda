package com.koitda.auth.dto;

import com.koitda.user.domain.RoleType;
import com.koitda.user.domain.User;
import java.util.List;

/** 가입·로그인 응답. Entity 를 직접 노출하지 않는다. */
public record AuthUserResponse(Long id, String nickname, List<String> roles) {

	public static AuthUserResponse from(User user) {
		List<String> roles = user.getRoles().stream().map(RoleType::name).sorted().toList();
		return new AuthUserResponse(user.getId(), user.getNickname(), roles);
	}
}
