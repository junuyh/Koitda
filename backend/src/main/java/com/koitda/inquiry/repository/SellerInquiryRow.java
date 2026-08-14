package com.koitda.inquiry.repository;

import java.time.OffsetDateTime;

/** 판매자 인박스 한 줄(JPQL 생성자 프로젝션). 판매자는 자기 도안 문의를 모두 볼 수 있다. */
public record SellerInquiryRow(
		Long id,
		Long patternId,
		String patternTitle,
		String content,
		String answer,
		boolean isPrivate,
		OffsetDateTime answeredAt,
		OffsetDateTime createdAt,
		String askerNickname) {
}
