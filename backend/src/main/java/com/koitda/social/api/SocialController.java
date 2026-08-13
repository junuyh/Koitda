package com.koitda.social.api;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.social.domain.TargetType;
import com.koitda.social.dto.SocialDtos.CommentItem;
import com.koitda.social.dto.SocialDtos.CommentListResponse;
import com.koitda.social.dto.SocialDtos.CreateCommentRequest;
import com.koitda.social.dto.SocialDtos.CreateReportRequest;
import com.koitda.social.dto.SocialDtos.FollowingItem;
import com.koitda.social.dto.SocialDtos.LikeResponse;
import com.koitda.social.dto.SocialDtos.ReportResponse;
import com.koitda.social.dto.SocialDtos.TargetRef;
import com.koitda.social.service.SocialService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 소셜(좋아요·댓글·팔로우·신고). 목록·수 조회는 공개, 나머지는 로그인 필요. */
@RestController
@RequestMapping("/api/v1")
public class SocialController {

	private final SocialService socialService;

	public SocialController(SocialService socialService) {
		this.socialService = socialService;
	}

	/** 좋아요 토글(SOCIAL-001). */
	@PostMapping("/likes")
	public LikeResponse toggleLike(@RequestBody TargetRef ref,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return socialService.toggleLike(principal.getUserId(), ref.targetType(), ref.targetId());
	}

	/** 댓글 목록(SOCIAL-002) — 비로그인 허용. */
	@GetMapping("/comments")
	public CommentListResponse comments(@RequestParam TargetType targetType, @RequestParam Long targetId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return socialService.listComments(targetType, targetId, principal != null ? principal.getUserId() : null);
	}

	/** 댓글 등록(SOCIAL-003) — 금칙어 검사. */
	@PostMapping("/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public CommentItem addComment(@RequestBody CreateCommentRequest req,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return socialService.addComment(principal.getUserId(), req.targetType(), req.targetId(), req.content());
	}

	@DeleteMapping("/comments/{commentId}")
	public void deleteComment(@PathVariable Long commentId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		socialService.deleteComment(principal.getUserId(), commentId);
	}

	/** 팔로우(SOCIAL-004). */
	@PostMapping("/follows/{userId}")
	public void follow(@PathVariable Long userId, @AuthenticationPrincipal CustomUserDetails principal) {
		socialService.follow(principal.getUserId(), userId);
	}

	@DeleteMapping("/follows/{userId}")
	public void unfollow(@PathVariable Long userId, @AuthenticationPrincipal CustomUserDetails principal) {
		socialService.unfollow(principal.getUserId(), userId);
	}

	@GetMapping("/users/me/following")
	public List<FollowingItem> following(@AuthenticationPrincipal CustomUserDetails principal) {
		return socialService.listFollowing(principal.getUserId());
	}

	@GetMapping("/users/me/followers")
	public List<FollowingItem> followers(@AuthenticationPrincipal CustomUserDetails principal) {
		return socialService.listFollowers(principal.getUserId());
	}

	/** 신고(REPORT-001~003). */
	@PostMapping("/reports")
	@ResponseStatus(HttpStatus.CREATED)
	public ReportResponse report(@RequestBody CreateReportRequest req,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return socialService.report(principal.getUserId(), req.targetType(), req.targetId(),
				req.reasonCode(), req.detail());
	}
}
