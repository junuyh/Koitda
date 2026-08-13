package com.koitda.social.repository;

import java.time.LocalDate;

/** 마이페이지 좋아요 탭 — 내가 좋아요한 오늘의 로그 투영. */
public interface MyLikedLogView {
	Long getId();

	String getDisplayTitle();

	Long getProjectId();

	LocalDate getLogDate();
}
