package com.koitda.inquiry.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * 도안 문의(문의하기). 도안 상세에서 구매 전 사용자가 판매자에게 질문한다.
 * pattern_id 는 참조. is_private 이면 작성자·해당 도안 판매자·관리자만 본문을 열람한다.
 * 답변(answer)은 판매자만 작성하며, 답변 시각·답변자를 함께 기록해 DB 제약으로 일관성을 강제한다.
 */
@Entity
@Table(name = "pattern_inquiry")
public class PatternInquiry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "pattern_id", nullable = false)
	private Long patternId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "is_private", nullable = false)
	private boolean isPrivate;

	@Column(name = "content", nullable = false)
	private String content;

	@Column(name = "answer")
	private String answer;

	@Column(name = "answered_at")
	private OffsetDateTime answeredAt;

	@Column(name = "answered_by")
	private Long answeredBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	protected PatternInquiry() {
	}

	public static PatternInquiry create(Long patternId, Long userId, String content, boolean isPrivate) {
		PatternInquiry q = new PatternInquiry();
		q.patternId = patternId;
		q.userId = userId;
		q.content = content;
		q.isPrivate = isPrivate;
		OffsetDateTime now = OffsetDateTime.now();
		q.createdAt = now;
		q.updatedAt = now;
		return q;
	}

	/** 판매자 답변 작성·수정. 답변 3종(answer·answeredAt·answeredBy)을 함께 채워 제약을 만족한다. */
	public void answer(String answer, Long sellerUserId) {
		this.answer = answer;
		this.answeredAt = OffsetDateTime.now();
		this.answeredBy = sellerUserId;
		this.updatedAt = this.answeredAt;
	}

	/** 작성자 본문 수정(미답변일 때만 허용). */
	public void editContent(String content, boolean isPrivate) {
		this.content = content;
		this.isPrivate = isPrivate;
		this.updatedAt = OffsetDateTime.now();
	}

	public void softDelete() {
		this.deletedAt = OffsetDateTime.now();
		this.updatedAt = this.deletedAt;
	}

	public boolean isOwnedBy(Long uid) {
		return userId != null && userId.equals(uid);
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}

	public boolean isAnswered() {
		return answeredAt != null;
	}

	public Long getId() {
		return id;
	}

	public Long getPatternId() {
		return patternId;
	}

	public Long getUserId() {
		return userId;
	}

	public boolean isPrivate() {
		return isPrivate;
	}

	public String getContent() {
		return content;
	}

	public String getAnswer() {
		return answer;
	}

	public OffsetDateTime getAnsweredAt() {
		return answeredAt;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
