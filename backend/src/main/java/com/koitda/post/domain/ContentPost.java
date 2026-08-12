package com.koitda.post.domain;

import com.koitda.project.domain.ProjectStatus;
import com.koitda.project.domain.ProjectVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 오늘의 로그 + 실타래(CONTENT_POST). 이번 슬라이스는 PROJECT_LOG 매핑에 집중. */
@Entity
@Table(name = "content_post")
public class ContentPost {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "post_type", nullable = false)
	private PostType postType;

	@Column(name = "project_id")
	private Long projectId;

	@Column(name = "title")
	private String title;

	@Column(name = "display_title", nullable = false)
	private String displayTitle;

	@Column(name = "title_sequence")
	private Integer titleSequence;

	@Column(name = "log_date")
	private LocalDate logDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "knitting_status")
	private ProjectStatus knittingStatus;

	@Column(name = "content_text")
	private String contentText;

	@org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
	@Column(name = "content_document")
	private String contentDocument;

	@Enumerated(EnumType.STRING)
	@Column(name = "visibility", nullable = false)
	private ProjectVisibility visibility = ProjectVisibility.PRIVATE;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	@Column(name = "purge_at")
	private OffsetDateTime purgeAt;

	protected ContentPost() {
	}

	/** 오늘의 로그 생성. */
	public static ContentPost forProjectLog(Long userId, Long projectId, ProjectStatus knittingStatus,
			String title, String displayTitle, Integer titleSequence, LocalDate logDate,
			String contentText, String contentDocument, ProjectVisibility visibility) {
		ContentPost post = new ContentPost();
		post.postType = PostType.PROJECT_LOG;
		post.userId = userId;
		post.projectId = projectId;
		post.knittingStatus = knittingStatus;
		post.title = title;
		post.displayTitle = displayTitle;
		post.titleSequence = titleSequence;
		post.logDate = logDate;
		post.contentText = contentText;
		post.contentDocument = contentDocument;
		post.visibility = visibility;
		return post;
	}

	@PrePersist
	void onCreate() {
		OffsetDateTime now = OffsetDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getDisplayTitle() {
		return displayTitle;
	}

	public LocalDate getLogDate() {
		return logDate;
	}

	public ProjectStatus getKnittingStatus() {
		return knittingStatus;
	}

	public String getContentText() {
		return contentText;
	}

	public String getContentDocument() {
		return contentDocument;
	}

	public ProjectVisibility getVisibility() {
		return visibility;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getProjectId() {
		return projectId;
	}

	public PostType getPostType() {
		return postType;
	}

	public String getTitle() {
		return title;
	}
}
