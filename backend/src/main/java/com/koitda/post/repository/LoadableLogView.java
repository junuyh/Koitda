package com.koitda.post.repository;

import java.time.LocalDate;

/** 리뷰 불러오기 후보 로그(REVIEW-005) 프로젝션. */
public interface LoadableLogView {

	Long getId();

	String getDisplayTitle();

	LocalDate getLogDate();

	String getKnittingStatus();

	Long getProjectId();
}
