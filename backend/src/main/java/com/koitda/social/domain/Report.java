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

/** 신고(REPORT-001~003). UNIQUE(reporter,target) 가 계정당 1회를 보장한다. */
@Entity
@Table(name = "report")
public class Report {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "reporter_id", nullable = false)
	private Long reporterId;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false)
	private TargetType targetType;

	@Column(name = "target_id", nullable = false)
	private Long targetId;

	@Column(name = "reason_code")
	private String reasonCode;

	@Column(name = "detail")
	private String detail;

	@Column(name = "is_valid", nullable = false)
	private boolean valid = true;

	@Column(name = "status", nullable = false)
	private String status = "PENDING";

	@Column(name = "created_at", nullable = false, updatable = false, insertable = false)
	private OffsetDateTime createdAt;

	protected Report() {
	}

	public static Report create(Long reporterId, TargetType targetType, Long targetId, String reasonCode,
			String detail) {
		Report r = new Report();
		r.reporterId = reporterId;
		r.targetType = targetType;
		r.targetId = targetId;
		r.reasonCode = reasonCode;
		r.detail = detail;
		r.valid = true;
		r.status = "PENDING";
		return r;
	}

	public Long getId() {
		return id;
	}

	public String getReasonCode() {
		return reasonCode;
	}

	public String getDetail() {
		return detail;
	}

	public java.time.OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
