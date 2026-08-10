package com.koitda.pattern.domain;

/** 도안 판매 상태. 카탈로그 노출은 APPROVED 만. */
public enum ProductStatus {
	DRAFT,
	PENDING,
	APPROVED,
	REJECTED,
	SUSPENDED
}
