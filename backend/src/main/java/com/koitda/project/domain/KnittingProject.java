package com.koitda.project.domain;

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
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 니팅로그(ERD KNITTING_PROJECT). 도안 XOR 외부도안, 상태는 최신 로그 파생 캐시.
 * pattern_snapshot 은 연결 시점 원작 정보의 복사본(외부도안이면 null).
 */
@Entity
@Table(name = "knitting_project")
public class KnittingProject {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "selling_pattern_id")
	private Long sellingPatternId;

	@Column(name = "external_pattern_id")
	private Long externalPatternId;

	@Column(name = "title")
	private String title;

	@Column(name = "display_title", nullable = false)
	private String displayTitle;

	@Column(name = "title_sequence")
	private Integer titleSequence;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private ProjectStatus status = ProjectStatus.PLANNED;

	@Enumerated(EnumType.STRING)
	@Column(name = "visibility", nullable = false)
	private ProjectVisibility visibility = ProjectVisibility.PRIVATE;

	@Column(name = "public_log_count", nullable = false)
	private int publicLogCount = 0;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "pattern_snapshot")
	private String patternSnapshot;

	@Column(name = "pattern_version_no")
	private Integer patternVersionNo;

	@Column(name = "snapshot_at")
	private OffsetDateTime snapshotAt;

	@Column(name = "note")
	private String note;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	@Column(name = "purge_at")
	private OffsetDateTime purgeAt;

	protected KnittingProject() {
	}

	private KnittingProject(Long userId, String title, String displayTitle, Integer titleSequence,
			ProjectVisibility visibility, String note) {
		this.userId = userId;
		this.title = title;
		this.displayTitle = displayTitle;
		this.titleSequence = titleSequence;
		this.visibility = visibility;
		this.note = note;
	}

	/** 판매/구매 도안 연결 — 원작 정보 스냅샷을 복사해 저장한다(PROJECT-017). */
	public static KnittingProject forCatalog(Long userId, Long sellingPatternId, String title,
			String displayTitle, Integer titleSequence, ProjectVisibility visibility, String note,
			String patternSnapshot, Integer patternVersionNo) {
		KnittingProject p = new KnittingProject(userId, title, displayTitle, titleSequence, visibility, note);
		p.sellingPatternId = sellingPatternId;
		p.patternSnapshot = patternSnapshot;
		p.patternVersionNo = patternVersionNo;
		p.snapshotAt = OffsetDateTime.now();
		return p;
	}

	/** 외부 도안 연결 — 스냅샷을 만들지 않는다. */
	public static KnittingProject forExternal(Long userId, Long externalPatternId, String title,
			String displayTitle, Integer titleSequence, ProjectVisibility visibility, String note) {
		KnittingProject p = new KnittingProject(userId, title, displayTitle, titleSequence, visibility, note);
		p.externalPatternId = externalPatternId;
		return p;
	}

	/** 최신 로그에서 파생한 상태로 갱신(POST-007). 값이 같으면 호출측에서 저장을 생략할 수 있다. */
	public void changeStatus(ProjectStatus status) {
		this.status = status;
	}

	/** 공개 로그 수 증가. 공개 불변식은 DB CHECK 가 함께 지킨다. */
	public void increasePublicLogCount() {
		this.publicLogCount++;
	}

	/** 공개 로그 수 감소(공개 로그 삭제 시). 0 미만으로 내려가지 않는다. */
	public void decreasePublicLogCount() {
		if (this.publicLogCount > 0) {
			this.publicLogCount--;
		}
	}

	/** 상향 전파 — 니팅로그를 공개로 전환(POST-011). */
	public void publish() {
		this.visibility = ProjectVisibility.PUBLIC;
	}

	/**
	 * 하향 전파 — 비공개 전환(PROJECT-019). 공개 로그 수를 0으로 함께 내려
	 * 공개 불변식(visibility=PUBLIC OR public_log_count=0)을 한 UPDATE 로 만족시킨다.
	 */
	public void makePrivate() {
		this.visibility = ProjectVisibility.PRIVATE;
		this.publicLogCount = 0;
	}

	/** 휴지통 이동(논리 삭제) — 90일 뒤 완전 삭제 예정 시각을 함께 기록(DATA-003). */
	public void moveToTrash() {
		OffsetDateTime now = OffsetDateTime.now();
		this.deletedAt = now;
		this.purgeAt = now.plusDays(90);
	}

	/** 복구 — 삭제·완전삭제 예정 시각을 지운다. */
	public void restore() {
		this.deletedAt = null;
		this.purgeAt = null;
	}

	public OffsetDateTime getDeletedAt() {
		return deletedAt;
	}

	public OffsetDateTime getPurgeAt() {
		return purgeAt;
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

	public Long getUserId() {
		return userId;
	}

	public Long getSellingPatternId() {
		return sellingPatternId;
	}

	public Long getExternalPatternId() {
		return externalPatternId;
	}

	public String getTitle() {
		return title;
	}

	public String getDisplayTitle() {
		return displayTitle;
	}

	public ProjectStatus getStatus() {
		return status;
	}

	public ProjectVisibility getVisibility() {
		return visibility;
	}

	public int getPublicLogCount() {
		return publicLogCount;
	}

	public String getPatternSnapshot() {
		return patternSnapshot;
	}

	public String getNote() {
		return note;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
