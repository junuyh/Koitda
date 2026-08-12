package com.koitda.project.repository;

import java.time.Instant;

/** 공개 니팅로그 피드(둘러보기) 투영. 좋아요수는 소속 공개 오늘의 로그 합산. */
public interface ProjectFeedView {
	Long getId();

	String getDisplayTitle();

	String getStatus();

	String getAuthorNickname();

	/** 대표 이미지(sort_order 최소) file_asset id. 없으면 null. */
	Long getThumbnailFileId();

	Long getLikeCount();

	Instant getCreatedAt();
}
