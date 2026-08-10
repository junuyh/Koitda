package com.koitda.pattern.dto;

import com.koitda.pattern.domain.PatternCategory;

public record CategoryResponse(Long id, String name, Long parentId, int sortOrder) {

	public static CategoryResponse from(PatternCategory c) {
		return new CategoryResponse(c.getId(), c.getName(), c.getParentId(), c.getSortOrder());
	}
}
