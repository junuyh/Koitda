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

/** 니팅로그 게이지. GAUGE-004 의 '내 게이지' 기본값 출처. */
@Entity
@Table(name = "project_gauge")
public class ProjectGauge {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "stitches")
	private BigDecimal stitches;

	@Column(name = "rows")
	private BigDecimal rows;

	@Column(name = "swatch_width_cm")
	private BigDecimal swatchWidthCm;

	@Column(name = "swatch_height_cm")
	private BigDecimal swatchHeightCm;

	@Column(name = "needle_size_mm")
	private BigDecimal needleSizeMm;

	@Column(name = "measured_stage")
	private String measuredStage;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Enumerated(EnumType.STRING)
	@Column(name = "input_source", nullable = false)
	private InputSource inputSource = InputSource.MANUAL;

	protected ProjectGauge() {
	}

	public ProjectGauge(Long projectId, BigDecimal stitches, BigDecimal rows, BigDecimal swatchWidthCm,
			BigDecimal swatchHeightCm, BigDecimal needleSizeMm, String measuredStage, int sortOrder) {
		this.projectId = projectId;
		this.stitches = stitches;
		this.rows = rows;
		this.swatchWidthCm = swatchWidthCm;
		this.swatchHeightCm = swatchHeightCm;
		this.needleSizeMm = needleSizeMm;
		this.measuredStage = measuredStage;
		this.sortOrder = sortOrder;
	}

	public Long getId() {
		return id;
	}

	public BigDecimal getStitches() {
		return stitches;
	}

	public BigDecimal getRows() {
		return rows;
	}

	public BigDecimal getNeedleSizeMm() {
		return needleSizeMm;
	}

	public String getMeasuredStage() {
		return measuredStage;
	}
}
