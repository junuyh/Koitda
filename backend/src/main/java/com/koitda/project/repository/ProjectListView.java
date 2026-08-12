package com.koitda.project.repository;

import java.time.Instant;

/** 내 니팅로그 플랫 목록 투영. 네이티브 조회는 TIMESTAMPTZ 를 Instant 로 준다. */
public interface ProjectListView {
	Long getId();

	String getDisplayTitle();

	String getStatus();

	String getVisibility();

	String getPatternTitle();

	Long getSellingPatternId();

	Long getExternalPatternId();

	/** 대표 이미지(sort_order 최소) file_asset id. 없으면 null. */
	Long getThumbnailFileId();

	Instant getCreatedAt();
}
