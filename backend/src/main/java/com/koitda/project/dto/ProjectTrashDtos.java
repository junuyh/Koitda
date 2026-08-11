package com.koitda.project.dto;

import com.koitda.project.domain.KnittingProject;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/** 휴지통 관련 응답. */
public final class ProjectTrashDtos {

	private ProjectTrashDtos() {
	}

	/** 휴지통 이동 결과 — 함께 삭제된 로그 수와 자동 삭제 예정일(DATA-003). */
	public record TrashResult(int connectedLogCount, OffsetDateTime purgeAt) {
	}

	public record TrashItemResponse(Long id, String displayTitle, OffsetDateTime deletedAt,
			OffsetDateTime purgeAt, long remainingDays) {

		public static TrashItemResponse from(KnittingProject p) {
			long remaining = p.getPurgeAt() == null ? 0
					: Math.max(0, ChronoUnit.DAYS.between(OffsetDateTime.now(), p.getPurgeAt()));
			return new TrashItemResponse(p.getId(), p.getDisplayTitle(),
					p.getDeletedAt(), p.getPurgeAt(), remaining);
		}
	}
}
