package com.koitda.project.dto;

import com.koitda.project.repository.ProjectListView;
import java.time.OffsetDateTime;

public record ProjectListItemResponse(
		Long id,
		String displayTitle,
		String status,
		String visibility,
		String patternTitle,
		String patternType,
		OffsetDateTime createdAt) {

	public static ProjectListItemResponse from(ProjectListView v) {
		String type = v.getExternalPatternId() != null ? "EXTERNAL" : "CATALOG";
		return new ProjectListItemResponse(v.getId(), v.getDisplayTitle(), v.getStatus(),
				v.getVisibility(), v.getPatternTitle(), type, v.getCreatedAt());
	}
}
