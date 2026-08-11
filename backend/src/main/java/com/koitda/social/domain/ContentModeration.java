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

	public int getValidReportCount() {
		return validReportCount;
	}

	public boolean isFlagged() {
		return flagged;
	}

	public boolean isAutoHidden() {
		return autoHidden;
	}
}
