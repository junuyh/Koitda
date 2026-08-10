package com.koitda.common.error;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** 모든 컨트롤러 예외를 공통 형식(code·message·fieldErrors·traceId)으로 변환한다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex) {
		ErrorCode code = ex.getErrorCode();
		String traceId = newTraceId();
		return ResponseEntity.status(code.status())
				.body(new ApiErrorResponse(code.name(), ex.getMessage(), Map.of(), traceId));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new HashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(fe -> fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
		ErrorCode code = ErrorCode.VALIDATION_ERROR;
		return ResponseEntity.status(code.status())
				.body(new ApiErrorResponse(code.name(), "입력값이 올바르지 않습니다.", fieldErrors, newTraceId()));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		// 잘못된 쿼리 파라미터 값(예: craftType=SEWING) → 400
		ErrorCode code = ErrorCode.VALIDATION_ERROR;
		Map<String, String> fieldErrors = Map.of(ex.getName(), "허용되지 않는 값입니다.");
		return ResponseEntity.status(code.status())
				.body(new ApiErrorResponse(code.name(), "요청 파라미터가 올바르지 않습니다.", fieldErrors, newTraceId()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
		String traceId = newTraceId();
		// 개인정보·민감정보가 남지 않도록 메시지 원문은 서버 로그에만 traceId 와 함께 남긴다.
		log.error("Unhandled exception traceId={}", traceId, ex);
		ErrorCode code = ErrorCode.INTERNAL_ERROR;
		return ResponseEntity.status(code.status())
				.body(new ApiErrorResponse(code.name(), "서버 오류가 발생했습니다.", Map.of(), traceId));
	}

	private String newTraceId() {
		return UUID.randomUUID().toString();
	}
}
