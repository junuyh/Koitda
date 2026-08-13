package com.koitda.review.api;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.review.dto.ReviewDtos.CreateReviewRequest;
import com.koitda.review.dto.ReviewDtos.LoadableLogItem;
import com.koitda.review.dto.ReviewDtos.ReviewCreatedResponse;
import com.koitda.review.dto.ReviewDtos.ReviewDeletedResponse;
import com.koitda.review.dto.ReviewDtos.ReviewDetail;
import com.koitda.review.dto.ReviewDtos.ReviewListResponse;
import com.koitda.review.dto.ReviewDtos.UpdateReviewRequest;
import com.koitda.review.service.ReviewService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 도안 리뷰(REVIEW-001~008). 목록·상세는 공개, 작성·수정·삭제·불러오기는 로그인 필요. */
@RestController
public class ReviewController {

	private final ReviewService reviewService;

	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	/** 공개 리뷰 목록 + 내 작성 여부. 비로그인 허용. */
	@GetMapping("/api/v1/patterns/{patternId}/reviews")
	public ReviewListResponse list(@PathVariable Long patternId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return reviewService.list(patternId, principal != null ? principal.getUserId() : null);
	}

	/** 내가 쓴 리뷰 목록(나의 뜨개방). 로그인 필요. */
	@GetMapping("/api/v1/users/me/reviews")
	public List<com.koitda.review.dto.ReviewDtos.MyReviewItem> myReviews(
			@AuthenticationPrincipal CustomUserDetails principal) {
		return reviewService.myReviews(principal.getUserId());
	}

	/** 불러오기 후보 로그(REVIEW-005). 로그인 필요. */
	@GetMapping("/api/v1/patterns/{patternId}/reviews/loadable-logs")
	public List<LoadableLogItem> loadableLogs(@PathVariable Long patternId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return reviewService.loadableLogs(principal.getUserId(), patternId);
	}

	/** 리뷰 작성 + 포인트 적립. */
	@PostMapping("/api/v1/patterns/{patternId}/reviews")
	@ResponseStatus(HttpStatus.CREATED)
	public ReviewCreatedResponse create(@PathVariable Long patternId,
			@RequestBody CreateReviewRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return reviewService.create(principal.getUserId(), patternId, request);
	}

	/** 리뷰 상세. 비로그인 허용(공개 리뷰만). */
	@GetMapping("/api/v1/reviews/{reviewId}")
	public ReviewDetail detail(@PathVariable Long reviewId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return reviewService.detail(reviewId, principal != null ? principal.getUserId() : null);
	}

	@PatchMapping("/api/v1/reviews/{reviewId}")
	public void update(@PathVariable Long reviewId, @RequestBody UpdateReviewRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		reviewService.update(reviewId, principal.getUserId(), request);
	}

	/** 리뷰 삭제 + 포인트 회수(POINT-003). */
	@DeleteMapping("/api/v1/reviews/{reviewId}")
	public ReviewDeletedResponse delete(@PathVariable Long reviewId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return reviewService.delete(reviewId, principal.getUserId());
	}
}
