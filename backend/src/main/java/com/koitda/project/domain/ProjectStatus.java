package com.koitda.project.domain;

/** 니팅로그 상태. 전이 순서를 강제하지 않고 유효 코드값만 검증한다(PROJECT-020). */
public enum ProjectStatus {
	PLANNED, // 준비 중
	CO,      // 코잡기
	WIP,     // 뜨는 중
	UFO,     // 잠시 멈춤
	FO       // 완성
}
