package com.koitda.project.dto;

import com.koitda.project.repository.ProjectListView;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ProjectListItemResponse(
		Long id,
		String displayTitle,
		String status,
		String visibility,
		String patternTitle,
		String patternType,
		String thumbnailUrl,
		OffsetDateTime createdAt) {

	public static ProjectListItemResponse from(ProjectListView v) {
		String type = v.getExternalPatternId() != null ? "EXTERNAL" : "CATALOG";
		String thumbnailUrl = v.getThumbnailFileId() != null ? "/api/v1/files/" + v.getThumbnailFileId() : null;
		return new ProjectListItemResponse(v.getId(), v.getDisplayTitle(), v.getStatus(),
				v.getVisibility(), v.getPatternTitle(), type, thumbnailUrl,
				v.getCreatedAt().atOffset(ZoneOffset.UTC));
	}
}
