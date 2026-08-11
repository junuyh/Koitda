package com.koitda.order.repository;

import java.time.Instant;

/** 구매 도안 목록 투영(LIBRARY-001). 네이티브 조회는 TIMESTAMPTZ 를 Instant 로 준다. */
public interface LibraryView {
	Long getPatternId();

	String getPatternTitle();

	String getCategoryName();

	Instant getPurchasedAt();

	boolean getRevoked();
}
