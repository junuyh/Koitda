package com.koitda.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

/** 팔로우(SOCIAL-004). 복합 PK + CHECK(follower<>following) 로 자기 팔로우를 막는다. */
@Entity
@Table(name = "follow")
@IdClass(Follow.Key.class)
public class Follow {

	@Id
	@Column(name = "follower_id")
	private Long followerId;

	@Id
	@Column(name = "following_id")
	private Long followingId;

	@Column(name = "created_at", nullable = false, updatable = false, insertable = false)
	private OffsetDateTime createdAt;

	protected Follow() {
	}

	public static Follow of(Long followerId, Long followingId) {
		Follow f = new Follow();
		f.followerId = followerId;
		f.followingId = followingId;
		return f;
	}

	public Long getFollowerId() {
		return followerId;
	}

	public Long getFollowingId() {
		return followingId;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public static class Key implements Serializable {
		private Long followerId;
		private Long followingId;

		public Key() {
		}

		public Key(Long followerId, Long followingId) {
			this.followerId = followerId;
			this.followingId = followingId;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Key k)) {
				return false;
			}
			return Objects.equals(followerId, k.followerId) && Objects.equals(followingId, k.followingId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(followerId, followingId);
		}
	}
}
