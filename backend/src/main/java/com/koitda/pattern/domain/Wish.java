package com.koitda.pattern.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

/** 위시(PATTERN-009). 복합 PK (user_id, pattern_id) 가 '계정당 1회'를 강제한다. */
@Entity
@Table(name = "wish")
public class Wish {

	@EmbeddedId
	private WishId id;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	protected Wish() {
	}

	public Wish(Long userId, Long patternId) {
		this.id = new WishId(userId, patternId);
		this.createdAt = OffsetDateTime.now();
	}

	public WishId getId() {
		return id;
	}

	@Embeddable
	public static class WishId implements Serializable {

		@Column(name = "user_id")
		private Long userId;

		@Column(name = "pattern_id")
		private Long patternId;

		protected WishId() {
		}

		public WishId(Long userId, Long patternId) {
			this.userId = userId;
			this.patternId = patternId;
		}

		public Long getUserId() {
			return userId;
		}

		public Long getPatternId() {
			return patternId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof WishId other)) {
				return false;
			}
			return Objects.equals(userId, other.userId) && Objects.equals(patternId, other.patternId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, patternId);
		}
	}
}
