package com.koitda.gauge.service;

import com.koitda.gauge.dto.GaugeDtos.CalculationResponse;
import com.koitda.gauge.dto.GaugeDtos.GaugeAdjustment;
import com.koitda.gauge.dto.GaugeDtos.GaugeInput;
import com.koitda.gauge.dto.GaugeDtos.NeedleRecommendation;
import com.koitda.gauge.dto.GaugeDtos.SizeAdjustment;
import com.koitda.gauge.dto.GaugeDtos.SizeInfo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 게이지 계산 순수 엔진(GAUGE-006·007·008·011·015). 상태 없음 · 부작용 없음 → 단위 테스트가 쉽다.
 * 2단계는 '시작 콧수 비례'가 아니라 '치수 기준'으로 계산한다(톱다운 요크에서 품 조정이 성립하도록).
 */
public final class GaugeCalculator {

	private GaugeCalculator() {
	}

	/** 콧수는 반올림, 치수는 소수 첫째 자리까지. */
	private static int roundInt(double v) {
		return BigDecimal.valueOf(v).setScale(0, RoundingMode.HALF_UP).intValue();
	}

	private static double round1(double v) {
		return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
	}

	/** 길이 계열(총장·소매길이)은 단(rows) 비율, 나머지(가슴둘레·어깨너비)는 코(stitches) 비율로 환산한다. */
	private static boolean isLength(String key) {
		return key != null && key.toLowerCase().contains("length");
	}

	private static final Map<String, String> SHORT_LABEL = Map.of(
			"chestCm", "품", "lengthCm", "기장", "sleeveLengthCm", "소매", "shoulderCm", "어깨");

	public static CalculationResponse calculate(GaugeInput patternGauge, GaugeInput myGauge, SizeInfo size,
			Map<String, Double> targetMeasurements, Map<String, String> labels) {
		double ps = patternGauge.stitches();
		double pr = patternGauge.rows();
		double ms = myGauge.stitches();
		double mr = myGauge.rows();
		Map<String, Double> targets = (targetMeasurements == null) ? Map.of() : targetMeasurements;
		Map<String, Double> patternMeas = (size.measurements() == null) ? Map.of() : size.measurements();

		// --- 1단계: 조정 콧수(GAUGE-006) ---
		// 도안에 시작 콧수가 있을 때만 계산한다. 없으면 null — 실제 계산은 치수 기준(2단계)이라 무관하다.
		Integer adjustedCastOn = null;
		String stage1Formula = null;
		if (size.castOnStitches() != null && size.castOnStitches() > 0) {
			int patternCastOn = size.castOnStitches();
			double adjRaw = patternCastOn * (ms / ps);
			adjustedCastOn = roundInt(adjRaw);
			stage1Formula = "%d × (%s ÷ %s) = %s → %d".formatted(
					patternCastOn, trim(ms), trim(ps), trim(round2(adjRaw)), adjustedCastOn);
		}

		// 예상 완성 치수(GAUGE-007): 도안 콧수를 내 게이지로 떴을 때. 폭=코비율, 길이=단비율.
		Map<String, Double> estimated = new LinkedHashMap<>();
		Map<String, Double> difference = new LinkedHashMap<>();
		for (Map.Entry<String, Double> e : patternMeas.entrySet()) {
			double ratio = isLength(e.getKey()) ? (pr / mr) : (ps / ms);
			double est = round1(e.getValue() * ratio);
			estimated.put(e.getKey(), est);
			difference.put(e.getKey(), round1(est - e.getValue()));
		}
		GaugeAdjustment gaugeAdjustment = new GaugeAdjustment(adjustedCastOn, estimated, difference, stage1Formula);

		// --- 2단계: 부위별 목표 치수에 필요한 콧수·단수(GAUGE-010·011) ---
		List<SizeAdjustment> sizeAdjustments = new ArrayList<>();
		List<String> summaryParts = new ArrayList<>();
		for (Map.Entry<String, Double> e : patternMeas.entrySet()) {
			String key = e.getKey();
			double patternValue = e.getValue();
			double targetValue = targets.getOrDefault(key, patternValue); // 미입력은 도안 값(GAUGE-010)
			double gauge10 = isLength(key) ? mr : ms; // 길이면 단 게이지, 폭이면 코 게이지
			double reqRaw = targetValue * gauge10 / 10.0;
			int required = roundInt(reqRaw);
			double diff = round1(targetValue - patternValue);
			String unit = isLength(key) ? "단" : "코";
			String formula = "%s × %s ÷ 10 = %s → %d%s".formatted(
					trim(targetValue), trim(gauge10), trim(round2(reqRaw)), required, unit);
			sizeAdjustments.add(new SizeAdjustment(key, labels.getOrDefault(key, key), patternValue, targetValue,
					diff, required, formula));
			if (Math.abs(diff) >= 0.05) { // 변경된 부위만 요약(GAUGE-015)
				summaryParts.add("%s %s%scm".formatted(
						SHORT_LABEL.getOrDefault(key, labels.getOrDefault(key, key)),
						diff > 0 ? "+" : "", trim(diff)));
			}
		}
		String adjustmentSummary = summaryParts.isEmpty() ? null : String.join(" · ", summaryParts);

		// --- 바늘 추천(GAUGE-008) — 참고값 ---
		NeedleRecommendation needle = recommendNeedle(patternGauge, ms, ps);

		// --- 경고 ---
		List<String> warnings = new ArrayList<>();
		double gaugeDiffPct = Math.abs(ms - ps) / ps * 100.0;
		if (gaugeDiffPct >= 10.0) {
			warnings.add("게이지 차이가 큽니다(약 %s%%). 스와치를 다시 확인하세요.".formatted(trim(round1(gaugeDiffPct))));
		}
		warnings.add("무늬 반복 단위를 확인하세요");

		return new CalculationResponse(null, gaugeAdjustment, sizeAdjustments, needle, adjustmentSummary, warnings);
	}

	private static NeedleRecommendation recommendNeedle(GaugeInput patternGauge, double ms, double ps) {
		Double patternNeedle = patternGauge.needleSizeMm();
		String note = "참고값이며 스와치 확인이 필요합니다";
		if (Math.abs(ms - ps) < 0.01) {
			return new NeedleRecommendation("SAME", patternNeedle, note);
		}
		// 내 게이지 코수가 크면(촘촘) 더 굵은 바늘로 도안 게이지에 맞춘다.
		boolean larger = ms > ps;
		Double suggested = (patternNeedle == null) ? null
				: round1(patternNeedle + (larger ? 0.5 : -0.5));
		return new NeedleRecommendation(larger ? "LARGER" : "SMALLER", suggested, note);
	}

	private static double round2(double v) {
		return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

	/** 정수면 소수점 제거해 계산식을 깔끔하게(예: 24.0 → 24, 24.5 → 24.5). */
	private static String trim(double v) {
		if (v == Math.rint(v)) {
			return String.valueOf((long) v);
		}
		return String.valueOf(v);
	}
}
