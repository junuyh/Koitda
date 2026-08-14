package com.koitda.review.service;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.order.domain.PatternLibrary;
import com.koitda.order.repository.PatternLibraryRepository;
import com.koitda.pattern.domain.ProductStatus;
import com.koitda.pattern.domain.SellingPattern;
import com.koitda.pattern.repository.SellingPatternRepository;
import com.koitda.point.service.PointService;
import com.koitda.point.service.PointService.EarnResult;
import com.koitda.point.service.PointService.RevokeResult;
import com.koitda.post.domain.ContentPost;
import com.koitda.post.domain.PostType;
import com.koitda.post.repository.ContentPostRepository;
import com.koitda.project.domain.KnittingProject;
import com.koitda.project.repository.KnittingProjectRepository;
import com.koitda.review.domain.PatternReview;
import com.koitda.review.dto.ReviewDtos.CreateReviewRequest;
import com.koitda.review.dto.ReviewDtos.LoadableLogItem;
import com.koitda.review.dto.ReviewDtos.MyReviewItem;
import com.koitda.review.dto.ReviewDtos.ReviewCreatedResponse;
import com.koitda.review.dto.ReviewDtos.ReviewDeletedResponse;
import com.koitda.review.dto.ReviewDtos.ReviewDetail;
import com.koitda.review.dto.ReviewDtos.ReviewItem;
import com.koitda.review.dto.ReviewDtos.ReviewListResponse;
import com.koitda.review.dto.ReviewDtos.UpdateReviewRequest;
import com.koitda.review.repository.PatternReviewRepository;
import com.koitda.social.domain.TargetType;
import com.koitda.social.service.SocialService;
import com.koitda.user.domain.User;
import com.koitda.user.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 도안 리뷰(REVIEW-001~008)와 포인트 연동(POINT-001·003).
 * 구매→기록→리뷰→포인트로 이어지는 핵심 순환의 마지막 고리다.
 */
@Service
public class ReviewService {

	private final PatternReviewRepository reviewRepository;
	private final SellingPatternRepository patternRepository;
	private final PatternLibraryRepository libraryRepository;
	private final ContentPostRepository postRepository;
	private final KnittingProjectRepository projectRepository;
	private final UserRepository userRepository;
	private final PointService pointService;
	private final SocialService socialService;
	private final com.koitda.review.repository.ReviewImageRepository reviewImageRepository;
	private final com.koitda.project.repository.ProjectImageRepository projectImageRepository;

	public ReviewService(PatternReviewRepository reviewRepository, SellingPatternRepository patternRepository,
			PatternLibraryRepository libraryRepository, ContentPostRepository postRepository,
			KnittingProjectRepository projectRepository, UserRepository userRepository, PointService pointService,
			SocialService socialService,
			com.koitda.review.repository.ReviewImageRepository reviewImageRepository,
			com.koitda.project.repository.ProjectImageRepository projectImageRepository) {
		this.reviewRepository = reviewRepository;
		this.patternRepository = patternRepository;
		this.libraryRepository = libraryRepository;
		this.postRepository = postRepository;
		this.projectRepository = projectRepository;
		this.userRepository = userRepository;
		this.pointService = pointService;
		this.socialService = socialService;
		this.reviewImageRepository = reviewImageRepository;
		this.projectImageRepository = projectImageRepository;
	}

	/** 리뷰 작성(REVIEW-002·003·004) + 최초 등록 포인트 적립(POINT-001). */
	@Transactional
	public ReviewCreatedResponse create(Long userId, Long patternId, CreateReviewRequest req) {
		requireApprovedPattern(patternId);

		// REVIEW-002: 구매 이력이 있어야 한다(환불 후에도 근거 유지).
		PatternLibrary library = libraryRepository
				.findFirstByUserIdAndPatternIdOrderByPurchasedAtAsc(userId, patternId)
				.orElseThrow(() -> new ApiException(ErrorCode.NOT_PURCHASED, "구매한 도안에만 리뷰를 쓸 수 있습니다."));

		// REVIEW-003: 도안당 1개(사전 확인 + DB 부분 유니크 이중 방어).
		if (reviewRepository.existsByUserIdAndPatternIdAndDeletedAtIsNull(userId, patternId)) {
			throw new ApiException(ErrorCode.REVIEW_ALREADY_EXISTS, "이미 이 도안에 리뷰를 작성했습니다.");
		}

		// REVIEW-004: 오늘의 로그 불러오기(복사) 또는 신규.
		Long sourcePostId = null;
		Long sourceProjectId = null;
		String knittingStatus = null;
		String title = trimToNull(req.title());
		String contentText = trimToNull(req.contentText());
		String contentDocument = null;

		if (req.sourcePostId() != null) {
			ContentPost post = loadOwnProjectLog(userId, patternId, req.sourcePostId());
			sourcePostId = post.getId();
			sourceProjectId = post.getProjectId();
			knittingStatus = post.getKnittingStatus() != null ? post.getKnittingStatus().name() : null;
			if (title == null) {
				title = post.getTitle() != null ? post.getTitle() : post.getDisplayTitle();
			}
			if (contentText == null) {
				contentText = post.getContentText();
			}
		}

		Integer rating = validateRating(req.rating());

		// gauge_adjustment_summary(REVIEW-010)는 게이지 계산 기능 도입 시 원본에서 복사한다. 지금은 null.
		PatternReview review = PatternReview.create(userId, patternId, library.getId(), sourcePostId,
				sourceProjectId, title, contentDocument, contentText, null, knittingStatus, null, rating, req.visibility());
		try {
			reviewRepository.saveAndFlush(review);
		} catch (DataIntegrityViolationException e) {
			// 동시 이중 등록이 부분 유니크에 걸린 경우.
			throw new ApiException(ErrorCode.REVIEW_ALREADY_EXISTS, "이미 이 도안에 리뷰를 작성했습니다.");
		}
		reviewRepository.addPatternReviewCount(patternId, 1);

		// 로그에서 불러온 경우: 그 니팅로그의 대표 사진을 리뷰에 복사(file_id 만 공유).
		if (sourceProjectId != null) {
			int order = 0;
			for (var pi : projectImageRepository.findByProject(sourceProjectId)) {
				if (pi.getFile() != null) {
					reviewImageRepository.save(new com.koitda.review.domain.ReviewImage(
							review.getId(), pi.getFile().getId(), order++));
				}
			}
		}

		EarnResult earn = pointService.awardReviewPoint(userId, review.getId());
		return new ReviewCreatedResponse(review.getId(), review.getContentText(),
				review.getGaugeAdjustmentSummary(), earn.earned(), earn.balance());
	}

	/** 공개 리뷰 목록 + 현재 사용자 맥락(REVIEW-001·009). userId 는 비로그인 시 null. */
	/** 내가 쓴 리뷰 목록(나의 뜨개방 › 내 활동). */
	@Transactional(readOnly = true)
	public java.util.List<MyReviewItem> myReviews(Long userId) {
		return reviewRepository.findMyReviews(userId).stream().map(MyReviewItem::from).toList();
	}

	@Transactional(readOnly = true)
	public ReviewListResponse list(Long patternId, Long userId) {
		List<PatternReview> reviews = reviewRepository.findPublicByPattern(patternId);
		Map<Long, String> nicknames = nicknamesOf(reviews.stream().map(PatternReview::getUserId).toList());
		Map<Long, SocialService.Counts> counts = socialService.countsFor(
				TargetType.REVIEW, reviews.stream().map(PatternReview::getId).toList(), userId);
		Map<Long, List<String>> imagesByReview = imagesFor(reviews.stream().map(PatternReview::getId).toList());

		List<ReviewItem> items = reviews.stream().map(r -> {
			SocialService.Counts c = counts.getOrDefault(r.getId(), new SocialService.Counts(0, 0, false));
			return new ReviewItem(
					r.getId(), nicknames.getOrDefault(r.getUserId(), "탈퇴한 사용자"), r.getTitle(), r.getContentText(),
					r.getKnittingStatus(), r.getGaugeAdjustmentSummary(), r.getRating(),
					imagesByReview.getOrDefault(r.getId(), List.of()), c.likeCount(), c.commentCount(), c.liked(),
					userId != null && r.isOwnedBy(userId), r.getCreatedAt());
		}).toList();

		boolean purchased = userId != null && libraryRepository
				.findFirstByUserIdAndPatternIdOrderByPurchasedAtAsc(userId, patternId).isPresent();
		Long myReviewId = (userId == null) ? null : reviewRepository
				.findByUserIdAndPatternIdAndDeletedAtIsNull(userId, patternId)
				.map(PatternReview::getId).orElse(null);

		return new ReviewListResponse(items, purchased, myReviewId);
	}

	/** 불러오기 후보 로그(REVIEW-005) — 본인의 해당 도안 연결 로그(비공개 포함). */
	@Transactional(readOnly = true)
	public List<LoadableLogItem> loadableLogs(Long userId, Long patternId) {
		return postRepository.findLoadableLogs(userId, patternId).stream()
				.map(v -> new LoadableLogItem(v.getId(), v.getDisplayTitle(), v.getLogDate(), v.getKnittingStatus()))
				.toList();
	}

	@Transactional(readOnly = true)
	public ReviewDetail detail(Long reviewId, Long userId) {
		PatternReview r = reviewRepository.findById(reviewId)
				.filter(x -> !x.isDeleted())
				.orElseThrow(() -> new ApiException(ErrorCode.REVIEW_NOT_FOUND, "리뷰를 찾을 수 없습니다."));
		boolean mine = userId != null && r.isOwnedBy(userId);
		if (!r.isPubliclyVisible() && !mine) {
			throw new ApiException(ErrorCode.REVIEW_NOT_FOUND, "리뷰를 찾을 수 없습니다.");
		}
		String nickname = nicknamesOf(List.of(r.getUserId())).getOrDefault(r.getUserId(), "탈퇴한 사용자");
		return new ReviewDetail(r.getId(), nickname, r.getTitle(), r.getContentText(), r.getContentDocument(),
				r.getKnittingStatus(), r.getGaugeAdjustmentSummary(), r.getRating(),
				imagesFor(List.of(r.getId())).getOrDefault(r.getId(), List.of()), r.getVisibility(), mine, r.getCreatedAt());
	}

	/** 리뷰 수정(REVIEW-008) — 작성자만. */
	@Transactional
	public void update(Long reviewId, Long userId, UpdateReviewRequest req) {
		PatternReview r = ownedActiveReview(reviewId, userId);
		String title = req.title() != null ? trimToNull(req.title()) : r.getTitle();
		String contentText = req.contentText() != null ? trimToNull(req.contentText()) : r.getContentText();
		r.edit(title, contentText, req.visibility(), validateRating(req.rating()));
	}

	/** 리뷰 삭제(REVIEW-008) + 포인트 회수(POINT-003). 삭제 후 재작성 가능. */
	@Transactional
	public ReviewDeletedResponse delete(Long reviewId, Long userId) {
		PatternReview r = ownedActiveReview(reviewId, userId);
		r.softDelete();
		reviewRepository.addPatternReviewCount(r.getPatternId(), -1);
		RevokeResult revoke = pointService.revokeReviewPoint(userId, reviewId);
		return new ReviewDeletedResponse(revoke.revoked(), revoke.balance(), revoke.failReason());
	}

	// ---------------------------------------------------------------- 내부

	private void requireApprovedPattern(Long patternId) {
		patternRepository.findByIdAndProductStatus(patternId, ProductStatus.APPROVED)
				.orElseThrow(() -> new ApiException(ErrorCode.PATTERN_NOT_FOUND, "도안을 찾을 수 없습니다."));
	}

	private PatternReview ownedActiveReview(Long reviewId, Long userId) {
		PatternReview r = reviewRepository.findById(reviewId)
				.filter(x -> !x.isDeleted())
				.orElseThrow(() -> new ApiException(ErrorCode.REVIEW_NOT_FOUND, "리뷰를 찾을 수 없습니다."));
		if (!r.isOwnedBy(userId)) {
			throw new ApiException(ErrorCode.ACCESS_DENIED, "본인 리뷰만 수정·삭제할 수 있습니다.");
		}
		return r;
	}

	/** 불러올 로그가 본인의 것이며 그 니팅로그가 이 도안에 연결됐는지 검증. */
	private ContentPost loadOwnProjectLog(Long userId, Long patternId, Long postId) {
		ContentPost post = postRepository.findById(postId)
				.filter(p -> p.getPostType() == PostType.PROJECT_LOG)
				.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND, "불러올 로그를 찾을 수 없습니다."));
		if (!userId.equals(post.getUserId()) || post.getProjectId() == null) {
			throw new ApiException(ErrorCode.ACCESS_DENIED, "본인 로그만 불러올 수 있습니다.");
		}
		KnittingProject project = projectRepository.findByIdAndUserId(post.getProjectId(), userId)
				.orElseThrow(() -> new ApiException(ErrorCode.PROJECT_NOT_FOUND, "연결된 니팅로그를 찾을 수 없습니다."));
		if (!patternId.equals(project.getSellingPatternId())) {
			throw new ApiException(ErrorCode.ACCESS_DENIED, "이 도안에 연결된 로그만 불러올 수 있습니다.");
		}
		return post;
	}

	private Map<Long, String> nicknamesOf(List<Long> userIds) {
		Map<Long, String> map = new HashMap<>();
		if (userIds.isEmpty()) {
			return map;
		}
		for (User u : userRepository.findAllById(userIds)) {
			map.put(u.getId(), u.getNickname());
		}
		return map;
	}

	private static String trimToNull(String s) {
		return (s == null || s.isBlank()) ? null : s.trim();
	}

	/** 별점 검증 — null 은 미입력(허용), 값이 있으면 1~5. */
	private Integer validateRating(Integer rating) {
		if (rating == null) {
			return null;
		}
		if (rating < 1 || rating > 5) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "별점은 1~5 사이여야 합니다.");
		}
		return rating;
	}

	/** 리뷰 id 목록 → 리뷰별 이미지 URL 목록(N+1 방지 배치 조회). */
	private Map<Long, List<String>> imagesFor(List<Long> reviewIds) {
		Map<Long, List<String>> map = new HashMap<>();
		if (reviewIds.isEmpty()) {
			return map;
		}
		for (var img : reviewImageRepository.findByReviewIdInOrderBySortOrderAscIdAsc(reviewIds)) {
			map.computeIfAbsent(img.getReviewId(), k -> new java.util.ArrayList<>())
					.add("/api/v1/files/" + img.getFileId());
		}
		return map;
	}
}
