package com.koitda.post.dto;

import com.koitda.project.domain.ProjectStatus;
import com.koitda.project.domain.ProjectVisibility;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 오늘의 로그 작성(POST-001·003·010·011).
 * visibility 미지정 시 상위 니팅로그 설정을 상속한다.
 * 비공개 니팅로그에 공개 로그를 저장하려면 publishProjectConfirmed=true 로 니팅로그 공개에 동의해야 한다.
 */
public record CreateLogRequest(
		@NotNull ProjectStatus knittingStatus,
		String title,
		LocalDate logDate,
		String comment,
		ProjectVisibility visibility,
		boolean publishProjectConfirmed) {
}
