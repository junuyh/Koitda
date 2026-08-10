package com.koitda.post.dto;

/** 로그 작성 결과. 상태 파생 결과와 공개 전파 결과를 함께 반환한다. */
public record LogCreatedResponse(
		Long id,
		String displayTitle,
		String knittingStatus,
		String logVisibility,
		String projectStatus,
		boolean projectPublished) {
}
