package com.koitda.social.service;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.review.domain.PatternReview;
import com.koitda.review.repository.PatternReviewRepository;
import com.koitda.social.domain.ContentModeration;
import com.koitda.social.domain.Report;
import com.koitda.social.domain.TargetType;
import com.koitda.social.dto.ModerationDtos.ReportedItem;
import com.koitda.social.repository.ContentModerationRepository;
import com.koitda.social.repository.ReportRepository;
import com.koitda.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 신고 관리(ADMIN-002). 신고가 접수된 콘텐츠를 검토·숨김·무시·복원한다.
 * 현재 enforce 되는 대상은 리뷰(pattern_review.moderation_status) — 목록 조회가 HIDDEN 을 즉시 제외한다.
 * (게시물·댓글 신고는 후속: content_post 매핑·목록 필터 확장 필요)
 */
@Service
public class AdminModerationService {

	private static final int PREVIEW_LEN = 120;

	private final ContentModerationRepository moderationRepository;
	private final ReportRepository reportRepository;
	private final PatternReviewRepository reviewRepository;
	private final UserRepository userRepository;

	public AdminModerationService(ContentModerationRepository moderationRepository,
			ReportRepository reportRepository, PatternReviewRepository reviewRepository,
			UserRepository userRepository) {
		this.moderationRepository = moderationRepository;
		this.reportRepository = reportRepository;
		this.reviewRepository = reviewRepository;
		this.userRepository = userRepository;
	}

	/** 신고 큐. 기본은 미처리(PENDING). resolution 파라미터로 처리 이력도 조회 가능. */
	@Transactional(readOnly = true)
	public List<ReportedItem> list(String resolution) {
		String res = (resolution == null || resolution.isBlank()) ? "PENDING" : resolution;
		return moderationRepository.findByResolutionOrderByValidReportCountDesc(res).stream()
				.filter(m -> m.getValidReportCount() > 0)
				.filter(m -> m.getTargetType() == TargetType.REVIEW) // 현재 enforce 대상만 노출
				.map(this::toItem)
				.toList();
	}

	/** 숨김 처리 — 대상 콘텐츠를 즉시 비공개. */
	@Transactional
	public void hide(Long moderationId, Long adminId) {
		ContentModeration m = load(moderationId);
		if (m.getTargetType() == TargetType.REVIEW) {
			PatternReview r = reviewRepository.findById(m.getTargetId())
					.orElseThrow(() -> new ApiException(ErrorCode.REVIEW_NOT_FOUND, "리뷰를 찾을 수 없습니다."));
			r.hideByAdmin();
		} else {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "현재 리뷰만 숨김 처리할 수 있습니다.");
		}
		m.resolveHidden(adminId);
	}

	/** 무시(유지) — 검토했으나 문제없음. 신고 플래그 해제. */
	@Transactional
	public void dismiss(Long moderationId, Long adminId) {
		load(moderationId).resolveDismissed(adminId);
	}

	/** 숨김 해제(복원). */
	@Transactional
	public void restore(Long moderationId, Long adminId) {
		ContentModeration m = load(moderationId);
		if (m.getTargetType() == TargetType.REVIEW) {
			reviewRepository.findById(m.getTargetId()).ifPresent(PatternReview::restoreByAdmin);
		}
		m.resolveRestored(adminId);
	}

	// ---------------------------------------------------------------- 내부

	private ContentModeration load(Long id) {
		return moderationRepository.findById(id)
				.orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "신고 항목을 찾을 수 없습니다."));
	}

	private ReportedItem toItem(ContentModeration m) {
		List<Report> reports = reportRepository
				.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(m.getTargetType(), m.getTargetId());
		List<String> reasons = reports.stream().map(Report::getReasonCode).distinct().toList();
		OffsetDateTime last = reports.isEmpty() ? null : reports.get(0).getCreatedAt();

		String title = null;
		String preview = null;
		String nickname = null;
		if (m.getTargetType() == TargetType.REVIEW) {
			PatternReview r = reviewRepository.findById(m.getTargetId()).orElse(null);
			if (r != null) {
				title = r.getTitle();
				preview = truncate(r.getContentText());
				nickname = userRepository.findById(r.getUserId()).map(u -> u.getNickname()).orElse("탈퇴한 사용자");
			}
		}
		boolean hidden = "HIDDEN".equals(m.getResolution()) || m.isAutoHidden();
		return new ReportedItem(m.getId(), m.getTargetType().name(), m.getTargetId(),
				m.getValidReportCount(), m.isFlagged(), hidden, m.getResolution(),
				title, preview, nickname, reasons, last);
	}

	private static String truncate(String s) {
		if (s == null) {
			return null;
		}
		return s.length() <= PREVIEW_LEN ? s : s.substring(0, PREVIEW_LEN) + "…";
	}
}
