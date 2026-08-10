package com.koitda.project.repository;

/** 코잇기(도안 타래) 그룹 조회 결과 투영. 도안 타래는 테이블이 아니라 그룹 쿼리 결과다. */
public interface PatternGroupView {
	String getPatternTitle();

	Long getSellingPatternId();

	Long getExternalPatternId();

	long getProjectCount();
}
