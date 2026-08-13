package com.koitda.user.dto;

import com.koitda.post.repository.MyPostView;
import com.koitda.social.domain.Comment;
import com.koitda.social.repository.MyLikedLogView;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 마이페이지 › 내 활동 탭 응답 묶음. */
public final class MeActivityDtos {

	private MeActivityDtos() {
	}

	/** 내 게시글(공개 오늘의 로그). */
	public record MyPostItem(Long postId, Long projectId, String displayTitle,
			String knittingStatus, LocalDate logDate, OffsetDateTime createdAt) {
		public static MyPostItem from(MyPostView v) {
			return new MyPostItem(v.getId(), v.getProjectId(), v.getDisplayTitle(),
					v.getKnittingStatus(), v.getLogDate(), v.getCreatedAt());
		}
	}

	/** 내 댓글. */
	public record MyCommentItem(Long id, String content, OffsetDateTime createdAt) {
		public static MyCommentItem from(Comment c) {
			return new MyCommentItem(c.getId(), c.getContent(), c.getCreatedAt());
		}
	}

	/** 좋아요한 오늘의 로그. */
	public record MyLikedLogItem(Long postId, Long projectId, String displayTitle, LocalDate logDate) {
		public static MyLikedLogItem from(MyLikedLogView v) {
			return new MyLikedLogItem(v.getId(), v.getProjectId(), v.getDisplayTitle(), v.getLogDate());
		}
	}
}
