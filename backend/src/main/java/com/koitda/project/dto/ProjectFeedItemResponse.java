package com.koitda.project.dto;

import com.koitda.project.repository.ProjectFeedView;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 공개 니팅로그 피드(둘러보기) 카드 응답. */
public record ProjectFeedItemResponse(
		Long id,
		String displayTitle,
		String status,
		String authorNickname,
		String thumbnailUrl,
		long likeCount,
		OffsetDateTime createdAt) {

	public static ProjectFeedItemResponse from(ProjectFeedView v) {
		String thumbnailUrl = v.getThumbnailFileId() != null ? "/api/v1/files/" + v.getThumbnailFileId() : null;
		long likes = v.getLikeCount() != null ? v.getLikeCount() : 0L;
		return new ProjectFeedItemResponse(v.getId(), v.getDisplayTitle(), v.getStatus(),
				v.getAuthorNickname(), thumbnailUrl, likes, v.getCreatedAt().atOffset(ZoneOffset.UTC));
	}
}
