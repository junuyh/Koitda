package com.koitda.social.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** 관리자 신고 관리(ADMIN-002) 응답 DTO. */
public final class ModerationDtos {

	private ModerationDtos() {
	}

	/** 신고 큐 한 줄 — 대상 콘텐츠 미리보기 + 신고 사유·건수. */
	public record ReportedItem(
			Long moderationId,
			String targetType,
			Long targetId,
			int reportCount,
			boolean flagged,
			boolean hidden,
			String resolution,
			String title,
			String preview,
			String authorNickname,
			List<String> reasons,
			OffsetDateTime lastReportedAt) {
	}
}
