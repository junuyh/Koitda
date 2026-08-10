package com.koitda.post.dto;

/** 로그 작성 결과 + 재계산된 니팅로그 상태(POST-007). */
public record LogCreatedResponse(
		Long id,
		String displayTitle,
		String knittingStatus,
		String visibility,
		String projectStatus) {
}
