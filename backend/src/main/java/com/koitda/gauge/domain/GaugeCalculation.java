package com.koitda.gauge.domain;

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
 * 저장된 게이지 계산(GAUGE-013·016). 입력·결과·계산식을 스냅샷으로 보관해 검산·AI 전달의 근거로 삼는다.
 * 클라이언트는 calculationId 만으로 AI 를 요청할 수 있고 계산값을 되돌려 보낼 수 없다(GAUGE-016).
 */
@Entity
@Table(name = "gauge_calculation")
public class GaugeCalculation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "pattern_gauge")
	private String patternGauge;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "my_gauge")
	private String myGauge;

	@Column(name = "selected_size_label")
	private String selectedSizeLabel;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "target_measurements")
	private String targetMeasurements;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "result")
	private String result;

	@Column(name = "adjusted_cast_on_stitches")
	private Integer adjustedCastOnStitches;

	@Column(name = "adjustment_summary")
	private String adjustmentSummary;

	@Column(name = "has_adjustment", nullable = false)
	private boolean hasAdjustment;

	@Column(name = "is_applied", nullable = false)
	private boolean applied;

	@Column(name = "created_at", nullable = false, updatable = false, insertable = false)
	private OffsetDateTime createdAt;

	protected GaugeCalculation() {
	}

	public static GaugeCalculation create(Long projectId, String patternGauge, String myGauge,
			String selectedSizeLabel, String targetMeasurements, String result, Integer adjustedCastOnStitches,
			String adjustmentSummary, boolean hasAdjustment) {
		GaugeCalculation g = new GaugeCalculation();
		g.projectId = projectId;
		g.patternGauge = patternGauge;
		g.myGauge = myGauge;
		g.selectedSizeLabel = selectedSizeLabel;
		g.targetMeasurements = targetMeasurements;
		g.result = result;
		g.adjustedCastOnStitches = adjustedCastOnStitches;
		g.adjustmentSummary = adjustmentSummary;
		g.hasAdjustment = hasAdjustment;
		g.applied = false;
		return g;
	}

	public void apply() {
		this.applied = true;
	}

	public void unapply() {
		this.applied = false;
	}

	public Long getId() {
		return id;
	}

	public Long getProjectId() {
		return projectId;
	}

	public String getResult() {
		return result;
	}

	public String getMyGauge() {
		return myGauge;
	}

	public String getPatternGauge() {
		return patternGauge;
	}

	public String getSelectedSizeLabel() {
		return selectedSizeLabel;
	}

	public Integer getAdjustedCastOnStitches() {
		return adjustedCastOnStitches;
	}

	public String getAdjustmentSummary() {
		return adjustmentSummary;
	}

	public boolean isHasAdjustment() {
		return hasAdjustment;
	}

	public boolean isApplied() {
		return applied;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
