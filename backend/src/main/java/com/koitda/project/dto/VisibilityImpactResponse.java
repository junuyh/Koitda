package com.koitda.project.dto;

/** 공개 변경 영향 조회(POST-008, PROJECT-019). 비공개 전환 시 함께 비공개될 공개 로그 수. */
public record VisibilityImpactResponse(long affectedPublicLogCount) {
}
