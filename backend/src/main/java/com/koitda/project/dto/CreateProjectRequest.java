package com.koitda.project.dto;

import com.koitda.project.domain.ProjectVisibility;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 니팅로그 생성 요청(PROJECT-003~012).
 * connectionType=CATALOG → sellingPatternId 로 원작 정보 스냅샷을 복사.
 * connectionType=EXTERNAL → externalPatternId(재사용) 또는 externalPattern(신규 생성).
 */
public record CreateProjectRequest(
		@NotNull ConnectionType connectionType,
		Long sellingPatternId,
		Long externalPatternId,
		ExternalPatternInput externalPattern,
		String title,
		String note,
		ProjectVisibility visibility,
		List<YarnInput> yarns,
		List<NeedleInput> needles,
		List<GaugeInput> gauges) {

	public enum ConnectionType {
		CATALOG,
		EXTERNAL
	}

	public record ExternalPatternInput(
			String title, String creatorName, String purchasePlace, String purchaseUrl,
			Long purchasePrice, LocalDate purchaseDate, String memo, String craftType, Long categoryId) {
	}

	public record YarnInput(String brand, String yarnName, String color, String amount, String unit, String note) {
	}

	public record NeedleInput(String needleType, BigDecimal sizeMm, Integer lengthCm, String note) {
	}

	public record GaugeInput(BigDecimal stitches, BigDecimal rows, BigDecimal swatchWidthCm,
			BigDecimal swatchHeightCm, BigDecimal needleSizeMm, String measuredStage) {
	}

	public List<YarnInput> yarnsOrEmpty() {
		return yarns == null ? List.of() : yarns;
	}

	public List<NeedleInput> needlesOrEmpty() {
		return needles == null ? List.of() : needles;
	}

	public List<GaugeInput> gaugesOrEmpty() {
		return gauges == null ? List.of() : gauges;
	}
}
