package com.koitda.review.repository;

import java.time.OffsetDateTime;

/** 내 리뷰 목록(나의 뜨개방 › 내 활동) 투영 — 리뷰 + 대상 도안명. */
public interface MyReviewView {
	Long getId();

	Long getPatternId();

	String getPatternTitle();

	String getTitle();

	String getContentText();

	String getKnittingStatus();

	String getVisibility();

	int getLikeCount();

	OffsetDateTime getCreatedAt();
}
