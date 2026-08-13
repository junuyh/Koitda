package com.koitda.user;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.post.repository.ContentPostRepository;
import com.koitda.social.repository.CommentRepository;
import com.koitda.social.repository.PostLikeRepository;
import com.koitda.user.dto.MeActivityDtos.MyCommentItem;
import com.koitda.user.dto.MeActivityDtos.MyLikedLogItem;
import com.koitda.user.dto.MeActivityDtos.MyPostItem;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 마이페이지 › 내 활동 탭(내 게시글·내 댓글·좋아요). 모두 로그인 사용자 본인 스코프. */
@RestController
@RequestMapping("/api/v1/users/me")
public class MeActivityController {

	private final ContentPostRepository postRepository;
	private final CommentRepository commentRepository;
	private final PostLikeRepository likeRepository;

	public MeActivityController(ContentPostRepository postRepository, CommentRepository commentRepository,
			PostLikeRepository likeRepository) {
		this.postRepository = postRepository;
		this.commentRepository = commentRepository;
		this.likeRepository = likeRepository;
	}

	@GetMapping("/posts")
	@Transactional(readOnly = true)
	public List<MyPostItem> posts(@AuthenticationPrincipal CustomUserDetails principal) {
		return postRepository.findMyPublicPosts(principal.getUserId()).stream().map(MyPostItem::from).toList();
	}

	@GetMapping("/comments")
	@Transactional(readOnly = true)
	public List<MyCommentItem> comments(@AuthenticationPrincipal CustomUserDetails principal) {
		return commentRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(principal.getUserId())
				.stream().map(MyCommentItem::from).toList();
	}

	@GetMapping("/likes")
	@Transactional(readOnly = true)
	public List<MyLikedLogItem> likes(@AuthenticationPrincipal CustomUserDetails principal) {
		return likeRepository.findMyLikedLogs(principal.getUserId()).stream().map(MyLikedLogItem::from).toList();
	}
}
