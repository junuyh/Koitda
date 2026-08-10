package com.koitda.project.dto;

import com.koitda.project.domain.ProjectVisibility;
import jakarta.validation.constraints.NotNull;

/** 니팅로그 공개 설정 변경. 비공개 전환이 공개 로그에 영향을 줄 때 confirmed=true 로 확인한다. */
public record ChangeVisibilityRequest(
		@NotNull ProjectVisibility visibility,
		boolean confirmed) {
}
