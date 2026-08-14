package com.koitda.review.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class ReviewDtos {

	private ReviewDtos() {
	}

	/**
	 * 리뷰 작성(REVIEW-004).
	 *  · sourcePostId: 오늘의 로그를 불러와 본문·상태를 복사(그 로그의 니팅로그 사진도 복사).
	 *  · sourceProjectId: 니팅로그 상세에서 바로 등록 — 그 니팅로그의 대표 사진·상태를 복사.
	 *  · 둘 다 없으면 신규 작성. rating 1~5(선택).
	 */
	public record CreateReviewRequest(
			Long sourcePostId,
			Long sourceProjectId,
			String title,
			String contentText,
			Integer rating,
			String visibility) {
	}

	/** 작성 결과 — 적립 포인트·갱신 잔액 포함(POINT-001). */
	public record ReviewCreatedResponse(
			Long reviewId,
			String contentText,
			String gaugeAdjustmentSummary,
			int earnedPoint,
			int pointBalance) {
	}

	public record UpdateReviewRequest(
			String title,
			String contentText,
			Integer rating,
			String visibility) {
	}

	/** 리뷰 목록 한 줄(REVIEW-001·009). 좋아요·댓글 수는 실시간 집계. images 는 로그에서 복사한 사진 URL. */
	public record ReviewItem(
			Long id,
			String authorNickname,
			String title,
			String contentText,
			String knittingStatus,
			String gaugeAdjustmentSummary,
			Integer rating,
			List<String> images,
			long likeCount,
			long commentCount,
			boolean liked,
			boolean mine,
			OffsetDateTime createdAt) {
	}

	/** 목록 + 현재 사용자 맥락(작성 버튼 노출 판단용). */
	public record ReviewListResponse(
			List<ReviewItem> items,
			boolean purchased,
			Long myReviewId) {
	}

	/** 리뷰 상세. */
	public record ReviewDetail(
			Long id,
			String authorNickname,
			String title,
			String contentText,
			@JsonRawValue String contentDocument,
			String knittingStatus,
			String gaugeAdjustmentSummary,
			Integer rating,
			List<String> images,
			String visibility,
			boolean mine,
			OffsetDateTime createdAt) {
	}

	/** 불러오기 후보 로그(REVIEW-005). */
	public record LoadableLogItem(
			Long postId,
			String displayTitle,
			LocalDate logDate,
			String knittingStatus) {
	}

	/** 삭제 결과 — 포인트 회수 정보 포함(POINT-003). */
	/** 내 리뷰 목록 항목(나의 뜨개방). */
	public record MyReviewItem(
			Long id,
			Long patternId,
			String patternTitle,
			String title,
			String contentText,
			String knittingStatus,
			String visibility,
			int likeCount,
			java.time.OffsetDateTime createdAt) {

		public static MyReviewItem from(com.koitda.review.repository.MyReviewView v) {
			return new MyReviewItem(v.getId(), v.getPatternId(), v.getPatternTitle(), v.getTitle(),
					v.getContentText(), v.getKnittingStatus(), v.getVisibility(), v.getLikeCount(), v.getCreatedAt());
		}
	}

	public record ReviewDeletedResponse(
			int revokedPoint,
			int pointBalance,
			String revokeFailReason) {
	}
}
