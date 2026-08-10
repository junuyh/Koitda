package com.koitda.project.dto;

import com.koitda.project.repository.PatternGroupView;

/** 코잇기 그룹 1건 = 도안 타래. */
public record ProjectGroupResponse(
		String patternTitle,
		String patternType,
		Long sellingPatternId,
		Long externalPatternId,
		long projectCount) {

	public static ProjectGroupResponse from(PatternGroupView v) {
		String type = v.getExternalPatternId() != null ? "EXTERNAL" : "CATALOG";
		return new ProjectGroupResponse(v.getPatternTitle(), type,
				v.getSellingPatternId(), v.getExternalPatternId(), v.getProjectCount());
	}
}
