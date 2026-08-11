package com.koitda.common.error;

import org.springframework.http.HttpStatus;

/** API 공통 오류 코드. 응답 body 의 code 값이자 HTTP 상태의 근거다. */
public enum ErrorCode {

	VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
	EMAIL_DUPLICATED(HttpStatus.CONFLICT),
	NICKNAME_DUPLICATED(HttpStatus.CONFLICT),
	TERMS_REQUIRED(HttpStatus.BAD_REQUEST),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
	UNAUTHENTICATED(HttpStatus.UNAUTHORIZED),
	ACCESS_DENIED(HttpStatus.FORBIDDEN),
	PATTERN_NOT_FOUND(HttpStatus.NOT_FOUND),
	PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND),
	ORDER_NOT_FOUND(HttpStatus.NOT_FOUND),
	ALREADY_OWNED(HttpStatus.CONFLICT),
	APPLICATION_ALREADY_PENDING(HttpStatus.CONFLICT),
	APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND),
	INVALID_APPLICATION_STATE(HttpStatus.CONFLICT),
	CONFIRMATION_REQUIRED(HttpStatus.valueOf(428)),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

	private final HttpStatus status;

	ErrorCode(HttpStatus status) {
		this.status = status;
	}

	public HttpStatus status() {
		return status;
	}
}
