package com.koitda.post.dto;

import com.koitda.post.domain.ContentPost;
import java.time.LocalDate;

/** 오늘의 로그 목록·작성 응답. */
public record LogResponse(
		Long id,
		String displayTitle,
		String knittingStatus,
		LocalDate logDate,
		String visibility,
		String comment) {

	public static LogResponse from(ContentPost p) {
		return new LogResponse(p.getId(), p.getDisplayTitle(),
				p.getKnittingStatus() != null ? p.getKnittingStatus().name() : null,
				p.getLogDate(), p.getVisibility().name(), p.getContentText());
	}
}
