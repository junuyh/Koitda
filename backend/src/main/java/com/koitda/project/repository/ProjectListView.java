package com.koitda.project.repository;

import java.time.OffsetDateTime;

/** 내 니팅로그 플랫 목록 투영. */
public interface ProjectListView {
	Long getId();

	String getDisplayTitle();

	String getStatus();

	String getVisibility();

	String getPatternTitle();

	Long getExternalPatternId();

	OffsetDateTime getCreatedAt();
}
