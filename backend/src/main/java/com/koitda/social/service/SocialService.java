package com.koitda.social.service;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.social.domain.Comment;
import com.koitda.social.domain.ContentModeration;
import com.koitda.social.domain.Follow;
import com.koitda.social.domain.PostLike;
import com.koitda.social.domain.Report;
import com.koitda.social.domain.TargetType;
import com.koitda.social.dto.SocialDtos.CommentItem;
import com.koitda.social.dto.SocialDtos.CommentListResponse;
import com.koitda.social.dto.SocialDtos.FollowingItem;
import com.koitda.social.dto.SocialDtos.LikeResponse;
import com.koitda.social.dto.SocialDtos.ReportResponse;
import com.koitda.social.repository.CommentRepository;
import com.koitda.social.repository.ContentModerationRepository;
import com.koitda.social.repository.FollowRepository;
import com.koitda.social.repository.PostLikeRepository;
import com.koitda.social.repository.ReportRepository;
import com.koitda.user.domain.User;
import com.koitda.user.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 소셜(좋아요·댓글·팔로우·신고). 대상 다형성으로 리뷰·오늘의 로그·실타래에 공통 적용한다. */
@Service
public class SocialService {

	private final PostLikeRepository likeRepository;
	private final CommentRepository commentRepository;
	private final FollowRepository followRepository;
	private final ReportRepository reportRepository;
	private final ContentModerationRepository moderationRepository;
	private final UserRepository userRepository;

	public SocialService(PostLikeRepository likeRepository, CommentRepository commentRepository,
			FollowRepository followRepository, ReportRepository reportRepository,
			ContentModerationRepository moderationRepository, UserRepository userRepository) {
		this.likeRepository = likeRepository;
		this.commentRepository = commentRepository;
		this.followRepository = followRepository;
		this.reportRepository = reportRepository;
		this.moderationRepository = moderationRepository;
		this.userRepository = userRepository;
	}

	// ---------------------------------------------------------------- 좋아요(SOCIAL-001)

	/** 좋아요 토글. 계정당 1회이므로 있으면 해제, 없으면 추가한다. */
	@Transactional
	public LikeResponse toggleLike(Long userId, TargetType targetType, Long targetId) {
		PostLike.Key key = new PostLike.Key(userId, targetType, targetId);
		boolean liked;
		if (likeRepository.existsById(key)) {
			likeRepository.deleteById(key);
			liked = false;
		} else {
			likeRepository.save(PostLike.of(userId, targetType, targetId));
			liked = true;
		}
		return new LikeResponse(liked, likeRepository.countByTargetTypeAndTargetId(targetType, targetId));
	}

	// ---------------------------------------------------------------- 댓글(SOCIAL-002·003)

	/** 댓글 목록. 비로그인도 수와 내용을 본다(작성만 로그인 필요). */
	@Transactional(readOnly = true)
	public CommentListResponse listComments(TargetType targetType, Long targetId, Long userId) {
		List<Comment> comments = commentRepository
				.findByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtAsc(targetType, targetId);
		Map<Long, String> nicknames = nicknamesOf(comments.stream().map(Comment::getUserId).toList());
		List<CommentItem> items = comments.stream().map(c -> new CommentItem(
				c.getId(), nicknames.getOrDefault(c.getUserId(), "탈퇴한 사용자"), c.getContent(),
				userId != null && c.isOwnedBy(userId), c.getCreatedAt())).toList();
		return new CommentListResponse(items.size(), items);
	}

	/** 댓글 등록 + 금칙어 검사(SOCIAL-003). 금칙어가 있으면 차단하고 발견 표현을 알린다. */
	@Transactional
	public CommentItem addComment(Long userId, TargetType targetType, Long targetId, String content) {
		if (content == null || content.isBlank()) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "댓글 내용을 입력하세요.");
		}
		List<String> matches = BannedWords.findMatches(content);
		if (!matches.isEmpty()) {
			throw new ApiException(ErrorCode.BANNED_WORD, "금칙어가 포함되어 등록할 수 없습니다: " + String.join(", ", matches));
		}
		Comment saved = commentRepository.save(Comment.create(userId, targetType, targetId, content.trim()));
		String nickname = nicknamesOf(List.of(userId)).getOrDefault(userId, "나");
		return new CommentItem(saved.getId(), nickname, saved.getContent(), true, saved.getCreatedAt());
	}

	@Transactional
	public void deleteComment(Long userId, Long commentId) {
		Comment c = commentRepository.findById(commentId)
				.orElseThrow(() -> new ApiException(ErrorCode.COMMENT_NOT_FOUND, "댓글을 찾을 수 없습니다."));
		if (!c.isOwnedBy(userId)) {
			throw new ApiException(ErrorCode.ACCESS_DENIED, "본인 댓글만 삭제할 수 있습니다.");
		}
		c.softDelete();
	}

	// ---------------------------------------------------------------- 팔로우(SOCIAL-004)

	@Transactional
	public void follow(Long followerId, Long followingId) {
		if (followerId.equals(followingId)) {
			throw new ApiException(ErrorCode.CANNOT_FOLLOW_SELF, "자기 자신은 팔로우할 수 없습니다.");
		}
		if (!userRepository.existsById(followingId)) {
			throw new ApiException(ErrorCode.USER_NOT_FOUND, "대상 사용자를 찾을 수 없습니다.");
		}
		if (!followRepository.existsById(new Follow.Key(followerId, followingId))) {
			followRepository.save(Follow.of(followerId, followingId));
		}
	}

	@Transactional
	public void unfollow(Long followerId, Long followingId) {
		followRepository.deleteById(new Follow.Key(followerId, followingId));
	}

	@Transactional(readOnly = true)
	public List<FollowingItem> listFollowing(Long userId) {
		List<Follow> follows = followRepository.findByFollowerId(userId);
		Map<Long, String> nicknames = nicknamesOf(follows.stream().map(Follow::getFollowingId).toList());
		return follows.stream().map(f -> new FollowingItem(
				f.getFollowingId(), nicknames.getOrDefault(f.getFollowingId(), "탈퇴한 사용자"),
				f.getCreatedAt())).toList();
	}

	// ---------------------------------------------------------------- 신고(REPORT-001~003)

	@Transactional
	public ReportResponse report(Long reporterId, TargetType targetType, Long targetId, String reasonCode,
			String detail) {
		if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(reporterId, targetType, targetId)) {
			throw new ApiException(ErrorCode.ALREADY_REPORTED, "이미 신고한 콘텐츠입니다.");
		}
		Report saved = reportRepository.save(Report.create(reporterId, targetType, targetId, reasonCode, detail));
		ContentModeration mod = moderationRepository.findByTargetTypeAndTargetId(targetType, targetId)
				.orElseGet(() -> ContentModeration.start(targetType, targetId));
		mod.addValidReport();
		moderationRepository.save(mod);
		return new ReportResponse(mod.getValidReportCount(), mod.isFlagged(), mod.isAutoHidden());
	}

	// ---------------------------------------------------------------- 목록 카운트(리뷰·로그 enrich)

	public record Counts(long likeCount, long commentCount, boolean liked) {
	}

	/** 대상 목록의 좋아요·댓글 수와 내 좋아요 여부를 배치로 채운다(N+1 방지). */
	@Transactional(readOnly = true)
	public Map<Long, Counts> countsFor(TargetType type, List<Long> ids, Long userId) {
		Map<Long, Counts> result = new HashMap<>();
		if (ids.isEmpty()) {
			return result;
		}
		Map<Long, Long> likes = new HashMap<>();
		for (Object[] row : likeRepository.countByTargets(type, ids)) {
			likes.put((Long) row[0], (Long) row[1]);
		}
		Map<Long, Long> comments = new HashMap<>();
		for (Object[] row : commentRepository.countByTargets(type, ids)) {
			comments.put((Long) row[0], (Long) row[1]);
		}
		List<Long> likedIds = (userId == null) ? List.of()
				: likeRepository.findLikedTargetIds(userId, type, ids);
		for (Long id : ids) {
			result.put(id, new Counts(likes.getOrDefault(id, 0L), comments.getOrDefault(id, 0L),
					likedIds.contains(id)));
		}
		return result;
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
}
