package com.koitda.social.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** 댓글(SOCIAL-002·003). */
@Entity
@Table(name = "comment")
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false)
	private TargetType targetType;

	@Column(name = "target_id", nullable = false)
	private Long targetId;

	@Column(name = "content", nullable = false)
	private String content;

	@Column(name = "status", nullable = false)
	private String status = "NORMAL";

	@Column(name = "created_at", nullable = false, updatable = false, insertable = false)
	private OffsetDateTime createdAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	protected Comment() {
	}

	public static Comment create(Long userId, TargetType targetType, Long targetId, String content) {
		Comment c = new Comment();
		c.userId = userId;
		c.targetType = targetType;
		c.targetId = targetId;
		c.content = content;
		c.status = "NORMAL";
		return c;
	}

	public void softDelete() {
		this.deletedAt = OffsetDateTime.now();
	}

	public boolean isOwnedBy(Long uid) {
		return userId != null && userId.equals(uid);
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public String getContent() {
		return content;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
