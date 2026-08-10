package com.koitda.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "project_needle")
public class ProjectNeedle {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "needle_type")
	private String needleType;

	@Column(name = "size_mm")
	private BigDecimal sizeMm;

	@Column(name = "length_cm")
	private Integer lengthCm;

	@Column(name = "note")
	private String note;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Enumerated(EnumType.STRING)
	@Column(name = "input_source", nullable = false)
	private InputSource inputSource = InputSource.MANUAL;

	protected ProjectNeedle() {
	}

	public ProjectNeedle(Long projectId, String needleType, BigDecimal sizeMm, Integer lengthCm,
			String note, int sortOrder) {
		this.projectId = projectId;
		this.needleType = needleType;
		this.sizeMm = sizeMm;
		this.lengthCm = lengthCm;
		this.note = note;
		this.sortOrder = sortOrder;
	}

	public String getNeedleType() {
		return needleType;
	}

	public BigDecimal getSizeMm() {
		return sizeMm;
	}

	public Integer getLengthCm() {
		return lengthCm;
	}

	public String getNote() {
		return note;
	}
}
