package com.koitda.inquiry.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** 도안 문의(문의하기) 요청·응답 DTO. */
public final class InquiryDtos {

	private InquiryDtos() {
	}

	/** 문의 작성 요청. content 필수, isPrivate 기본 공개(false). */
	public record CreateInquiryRequest(String content, boolean isPrivate) {
	}

	/** 판매자 답변 요청. */
	public record AnswerInquiryRequest(String answer) {
	}

	/** 작성자 문의 수정 요청(미답변일 때만). */
	public record UpdateInquiryRequest(String content, boolean isPrivate) {
	}

	/**
	 * 도안 상세 문의 목록 한 줄. 비공개 문의를 열람 권한 없는 사용자가 보면
	 * locked=true 로 본문·답변을 가리고 존재만 노출한다.
	 */
	public record InquiryItem(
			Long id,
			String askerNickname,
			boolean isPrivate,
			boolean locked,
			String content,
			String answer,
			boolean answered,
			boolean mine,
			boolean canAnswer,
			OffsetDateTime createdAt,
			OffsetDateTime answeredAt) {
	}

	/** 도안 상세 문의 목록 응답 + 현재 사용자 맥락. */
	public record InquiryListResponse(
			List<InquiryItem> items,
			boolean canAsk,
			boolean isSeller) {
	}

	public record InquiryCreatedResponse(Long id) {
	}

	/** 판매자 인박스 한 줄. */
	public record SellerInquiryItem(
			Long id,
			Long patternId,
			String patternTitle,
			String askerNickname,
			boolean isPrivate,
			String content,
			String answer,
			boolean answered,
			OffsetDateTime createdAt,
			OffsetDateTime answeredAt) {
	}

	/** 판매자 인박스 응답 + 미답변 개수(배지). */
	public record SellerInboxResponse(List<SellerInquiryItem> items, long unansweredCount) {
	}
}
