package com.koitda.gauge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.koitda.gauge.dto.GaugeDtos.CalculationResponse;
import com.koitda.gauge.dto.GaugeDtos.GaugeInput;
import com.koitda.gauge.dto.GaugeDtos.SizeAdjustment;
import com.koitda.gauge.dto.GaugeDtos.SizeInfo;
import com.koitda.gauge.service.GaugeCalculator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 게이지 엔진 순수 단위 테스트: 자유 실측 한글 라벨의 길이/폭 게이지 구분. */
class GaugeCalculatorTest {

	@Test
	void 한글_길이라벨은_단게이지_폭라벨은_코게이지로_계산된다() {
		GaugeInput pattern = new GaugeInput(22.0, 30.0, 4.5);
		GaugeInput mine = new GaugeInput(24.0, 32.0, null);

		// 자유 실측: 폭(가슴둘레) + 길이(총장). 시작 콧수 없음.
		Map<String, Double> meas = new LinkedHashMap<>();
		meas.put("가슴둘레", 100.0);
		meas.put("총장", 60.0);
		SizeInfo size = new SizeInfo("M", null, meas);

		CalculationResponse r = GaugeCalculator.calculate(pattern, mine, size, Map.of(),
				Map.of("가슴둘레", "가슴둘레", "총장", "총장"));

		SizeAdjustment chest = find(r, "가슴둘레");
		SizeAdjustment length = find(r, "총장");

		// 폭: 100 × 24(코) ÷ 10 = 240코
		assertEquals(240, chest.requiredStitches());
		assertTrue(chest.formula().contains("코"), "폭은 코 게이지: " + chest.formula());

		// 길이(총장): 60 × 32(단) ÷ 10 = 192단  (수정 전엔 24코로 잘못 계산됐음)
		assertEquals(192, length.requiredStitches());
		assertTrue(length.formula().contains("단"), "길이는 단 게이지: " + length.formula());
	}

	private static SizeAdjustment find(CalculationResponse r, String key) {
		return r.sizeAdjustments().stream().filter(a -> a.key().equals(key)).findFirst().orElseThrow();
	}
}
