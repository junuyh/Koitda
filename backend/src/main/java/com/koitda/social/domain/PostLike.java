package com.koitda.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

/** 좋아요(SOCIAL-001). 복합 PK 가 계정당 1회를 보장한다. */
@Entity
@Table(name = "post_like")
@IdClass(PostLike.Key.class)
public class PostLike {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "target_type")
	private TargetType targetType;

	@Id
	@Column(name = "target_id")
	private Long targetId;

	@Column(name = "created_at", nullable = false, updatable = false, insertable = false)
	private OffsetDateTime createdAt;

	protected PostLike() {
	}

	public static PostLike of(Long userId, TargetType targetType, Long targetId) {
		PostLike l = new PostLike();
		l.userId = userId;
		l.targetType = targetType;
		l.targetId = targetId;
		return l;
	}

	public static class Key implements Serializable {
		private Long userId;
		private TargetType targetType;
		private Long targetId;

		public Key() {
		}

		public Key(Long userId, TargetType targetType, Long targetId) {
			this.userId = userId;
			this.targetType = targetType;
			this.targetId = targetId;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Key k)) {
				return false;
			}
			return Objects.equals(userId, k.userId) && targetType == k.targetType
					&& Objects.equals(targetId, k.targetId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, targetType, targetId);
		}
	}
}
