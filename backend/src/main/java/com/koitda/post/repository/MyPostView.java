package com.koitda.post.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 마이페이지 내 게시글(공개 오늘의 로그) 투영. */
public interface MyPostView {
	Long getId();

	String getDisplayTitle();

	Long getProjectId();

	String getKnittingStatus();

	LocalDate getLogDate();

	OffsetDateTime getCreatedAt();
}
