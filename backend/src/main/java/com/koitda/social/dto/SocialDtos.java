package com.koitda.social.dto;

import com.koitda.social.domain.TargetType;
import java.time.OffsetDateTime;
import java.util.List;

public final class SocialDtos {

	private SocialDtos() {
	}

	public record TargetRef(TargetType targetType, Long targetId) {
	}

	/** 좋아요 토글 결과. */
	public record LikeResponse(boolean liked, long likeCount) {
	}

	public record CreateCommentRequest(TargetType targetType, Long targetId, String content) {
	}

	public record CommentItem(Long id, String authorNickname, String content, boolean mine, OffsetDateTime createdAt) {
	}

	/** 댓글 목록 — 비로그인도 수는 본다(SOCIAL-002). */
	public record CommentListResponse(long count, List<CommentItem> items) {
	}

	public record FollowingItem(Long userId, String nickname, OffsetDateTime since) {
	}

	public record CreateReportRequest(TargetType targetType, Long targetId, String reasonCode, String detail) {
	}

	/** 신고 결과 — 누적 유효 신고 수·자동 숨김 여부(REPORT-002·003). */
	public record ReportResponse(int validReportCount, boolean flagged, boolean autoHidden) {
	}
}
