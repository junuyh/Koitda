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

/** 신고 집계·자동 숨김(REPORT-002·003). 대상당 한 행. */
@Entity
@Table(name = "content_moderation")
public class ContentModeration {

	/** 유효 신고 3건: 관리자 확인 대상 / 10건: 자동 숨김. */
	public static final int FLAG_THRESHOLD = 3;
	public static final int AUTO_HIDE_THRESHOLD = 10;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false)
	private TargetType targetType;

	@Column(name = "target_id", nullable = false)
	private Long targetId;

	@Column(name = "valid_report_count", nullable = false)
	private int validReportCount;

	@Column(name = "is_flagged", nullable = false)
	private boolean flagged;

	@Column(name = "is_auto_hidden", nullable = false)
	private boolean autoHidden;

	@Column(name = "hidden_at")
	private OffsetDateTime hiddenAt;

	/** 관리자 처리 상태: PENDING(대기)·HIDDEN(숨김)·DISMISSED(무시)·RESTORED(복원). */
	@Column(name = "resolution", nullable = false)
	private String resolution = "PENDING";

	@Column(name = "resolved_by")
	private Long resolvedBy;

	@Column(name = "resolved_at")
	private OffsetDateTime resolvedAt;

	protected ContentModeration() {
	}

	public static ContentModeration start(TargetType targetType, Long targetId) {
		ContentModeration m = new ContentModeration();
		m.targetType = targetType;
		m.targetId = targetId;
		m.validReportCount = 0;
		return m;
	}

	/** 유효 신고 1건 반영 후 임계치에 따라 플래그·자동숨김을 갱신한다. */
	public void addValidReport() {
		this.validReportCount++;
		if (this.validReportCount >= FLAG_THRESHOLD) {
			this.flagged = true;
		}
		if (this.validReportCount >= AUTO_HIDE_THRESHOLD && !this.autoHidden) {
			this.autoHidden = true;
			this.hiddenAt = OffsetDateTime.now();
		}
	}

	/** 관리자 숨김 처리. */
	public void resolveHidden(Long adminId) {
		this.resolution = "HIDDEN";
		this.autoHidden = true;
		this.hiddenAt = OffsetDateTime.now();
		this.resolvedBy = adminId;
		this.resolvedAt = OffsetDateTime.now();
	}

	/** 관리자 무시(유지) 처리 — 신고를 검토했으나 문제없음. */
	public void resolveDismissed(Long adminId) {
		this.resolution = "DISMISSED";
		this.flagged = false;
		this.resolvedBy = adminId;
		this.resolvedAt = OffsetDateTime.now();
	}

	/** 숨김 해제(복원). */
	public void resolveRestored(Long adminId) {
		this.resolution = "RESTORED";
		this.autoHidden = false;
		this.hiddenAt = null;
		this.resolvedBy = adminId;
		this.resolvedAt = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public TargetType getTargetType() {
		return targetType;
	}

	public Long getTargetId() {
		return targetId;
	}

	public int getValidReportCount() {
		return validReportCount;
	}

	public boolean isFlagged() {
		return flagged;
	}

	public boolean isAutoHidden() {
		return autoHidden;
	}

	public String getResolution() {
		return resolution;
	}
}
