package com.koitda.common.error;

import java.util.Map;

/** 오류 응답 공통 형식(API 공통 규칙). */
public record ApiErrorResponse(
		String code,
		String message,
		Map<String, String> fieldErrors,
		String traceId) {
}
