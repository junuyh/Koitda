package com.koitda.gauge.dto;

import java.util.List;
import java.util.Map;

/** 게이지 계산(GAUGE-001~017) 요청·응답. 계산은 서버 수식으로만 처리한다(AI-001). */
public final class GaugeDtos {

	private GaugeDtos() {
	}

	public record GaugeInput(Double stitches, Double rows, Double needleSizeMm) {
	}

	public record SizeInfo(String label, Integer castOnStitches, Map<String, Double> measurements) {
	}

	public record MyGaugeSuggestion(Long projectGaugeId, Double stitches, Double rows, String measuredStage) {
	}

	/** 계산 기본값(GAUGE-002·003·004·005). 외부 도안이면 requiresManualInput=true 로 도안값을 비운다. */
	public record GaugeDefaultsResponse(
			String patternSource,
			boolean requiresManualInput,
			GaugeInput patternGauge,
			List<SizeInfo> sizes,
			Map<String, String> measurementLabels,
			List<MyGaugeSuggestion> myGaugeSuggestions) {
	}

	public record CalculateRequest(
			Long projectId,
			GaugeInput patternGauge,
			GaugeInput myGauge,
			String selectedSizeLabel,
			Map<String, Double> targetMeasurements) {
	}

	/** 1단계 — 조정 콧수·예상 완성 치수(GAUGE-006·007). */
	public record GaugeAdjustment(
			int adjustedCastOnStitches,
			Map<String, Double> estimatedMeasurements,
			Map<String, Double> differenceFromPattern,
			String formula) {
	}

	/** 2단계 — 부위별 목표 치수에 필요한 콧수·단수(GAUGE-011). */
	public record SizeAdjustment(
			String key,
			String label,
			double patternValue,
			double targetValue,
			double differenceCm,
			int requiredStitches,
			String formula) {
	}

	public record NeedleRecommendation(String direction, Double suggestedMm, String note) {
	}

	/** 계산 결과 전체(GAUGE-009·012·015). result JSONB 로 저장되는 스냅샷. */
	public record CalculationResponse(
			Long calculationId,
			GaugeAdjustment gaugeAdjustment,
			List<SizeAdjustment> sizeAdjustments,
			NeedleRecommendation needleRecommendation,
			String adjustmentSummary,
			List<String> warnings) {
	}

	/** 니팅로그 상세용 요약(GAUGE-013·015). 적용 계산이 없으면 null. */
	public record AppliedCalculationSummary(
			Long calculationId,
			Integer adjustedCastOnStitches,
			String adjustmentSummary,
			boolean hasAdjustment) {
	}
}
