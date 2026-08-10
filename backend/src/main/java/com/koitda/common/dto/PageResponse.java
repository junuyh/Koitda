package com.koitda.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** 목록 API 공통 응답 형식 (API 공통 규칙: items·page·size·totalElements·totalPages). */
public record PageResponse<T>(
		List<T> items,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public static <T> PageResponse<T> of(Page<?> page, List<T> items) {
		return new PageResponse<>(items, page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}
}
