package com.koitda.project.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.koitda.project.domain.KnittingProject;

public record ProjectCreatedResponse(
		Long id,
		String displayTitle,
		String status,
		String visibility,
		@JsonRawValue String patternSnapshot) {

	public static ProjectCreatedResponse from(KnittingProject p) {
		return new ProjectCreatedResponse(p.getId(), p.getDisplayTitle(),
				p.getStatus().name(), p.getVisibility().name(), p.getPatternSnapshot());
	}
}
