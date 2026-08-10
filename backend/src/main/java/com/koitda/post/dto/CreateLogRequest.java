package com.koitda.post.dto;

import com.koitda.project.domain.ProjectStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 오늘의 로그 작성(POST-001·003). 블록 에디터 본문은 후속 슬라이스에서 확장하고,
 * 이번에는 상태·기록일·짧은 코멘트를 저장한다.
 */
public record CreateLogRequest(
		@NotNull ProjectStatus knittingStatus,
		String title,
		LocalDate logDate,
		String comment) {
}
