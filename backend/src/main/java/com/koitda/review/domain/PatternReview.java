package com.koitda.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 도안 리뷰(REVIEW-001~010). 오늘의 로그에서 '복사'해 독립시킨다 — 원본을 수정·삭제해도 리뷰는 그대로다.
 * visibility(작성자)와 moderation_status(관리자)는 권한 주체가 달라 컬럼을 분리한다.
 */
@Entity
@Table(name = "pattern_review")
public class PatternReview {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "pattern_id", nullable = false)
	private Long patternId;

	@Column(name = "library_id", nullable = false)
	private Long libraryId;

	@Column(name = "source_post_id")
	private Long sourcePostId;

	@Column(name = "source_project_id")
	private Long sourceProjectId;

	@Column(name = "title")
	private String title;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "content_document")
	private String contentDocument;

	@Column(name = "content_text")
	private String contentText;

	@Column(name = "cover_file_id")
	private Long coverFileId;

	@Column(name = "knitting_status")
	private String knittingStatus;

	@Column(name = "gauge_adjustment_summary")
	private String gaugeAdjustmentSummary;

	/** 별점 1~5(선택). 값이 있으면 DB CHECK 로 범위 강제. */
	@Column(name = "rating")
	private Integer rating;

	@Column(name = "visibility", nullable = false)
	private String visibility = "PUBLIC";

	@Column(name = "moderation_status", nullable = false)
	private String moderationStatus = "NORMAL";

	@Column(name = "like_count", nullable = false)
	private int likeCount;

	@Column(name = "comment_count", nullable = false)
	private int commentCount;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	protected PatternReview() {
	}

	public static PatternReview create(Long userId, Long patternId, Long libraryId, Long sourcePostId,
			Long sourceProjectId, String title, String contentDocument, String contentText, Long coverFileId,
			String knittingStatus, String gaugeAdjustmentSummary, Integer rating, String visibility) {
		PatternReview r = new PatternReview();
		r.userId = userId;
		r.patternId = patternId;
		r.libraryId = libraryId;
		r.sourcePostId = sourcePostId;
		r.sourceProjectId = sourceProjectId;
		r.title = title;
		r.contentDocument = contentDocument;
		r.contentText = contentText;
		r.coverFileId = coverFileId;
		r.knittingStatus = knittingStatus;
		r.gaugeAdjustmentSummary = gaugeAdjustmentSummary;
		r.rating = rating;
		r.visibility = (visibility == null) ? "PUBLIC" : visibility;
		r.moderationStatus = "NORMAL";
		OffsetDateTime now = OffsetDateTime.now();
		r.createdAt = now;
		r.updatedAt = now;
		return r;
	}

	/** 작성자 본인 수정(REVIEW-008). 원본 로그와 독립이라 자유 편집한다. */
	public void edit(String title, String contentText, String visibility, Integer rating) {
		this.title = title;
		this.contentText = contentText;
		if (visibility != null) {
			this.visibility = visibility;
		}
		if (rating != null) {
			this.rating = rating;
		}
		this.updatedAt = OffsetDateTime.now();
	}

	/** 논리 삭제(REVIEW-008). 삭제하면 재작성이 가능하다(부분 유니크가 deleted_at NULL 만 잠금). */
	public void softDelete() {
		this.deletedAt = OffsetDateTime.now();
		this.updatedAt = this.deletedAt;
	}

	public boolean isOwnedBy(Long uid) {
		return this.userId != null && this.userId.equals(uid);
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}

	public boolean isPubliclyVisible() {
		return "PUBLIC".equals(visibility) && "NORMAL".equals(moderationStatus) && deletedAt == null;
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getPatternId() {
		return patternId;
	}

	public Long getSourcePostId() {
		return sourcePostId;
	}

	public Long getSourceProjectId() {
		return sourceProjectId;
	}

	public String getTitle() {
		return title;
	}

	public String getContentDocument() {
		return contentDocument;
	}

	public String getContentText() {
		return contentText;
	}

	public String getKnittingStatus() {
		return knittingStatus;
	}

	public String getGaugeAdjustmentSummary() {
		return gaugeAdjustmentSummary;
	}

	public Integer getRating() {
		return rating;
	}

	public String getVisibility() {
		return visibility;
	}

	public int getLikeCount() {
		return likeCount;
	}

	public int getCommentCount() {
		return commentCount;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
