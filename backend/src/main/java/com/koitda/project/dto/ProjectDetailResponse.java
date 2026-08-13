package com.koitda.project.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** 니팅로그 상세(PROJECT-011·012). 원작 스냅샷은 JSONB 원문을 중첩 JSON 으로 노출. */
public record ProjectDetailResponse(
		Long id,
		String title,
		String displayTitle,
		String status,
		String visibility,
		int publicLogCount,
		String note,
		OffsetDateTime createdAt,
		String patternType,
		Long sellingPatternId,
		Long externalPatternId,
		@JsonRawValue String patternSnapshot,
		ExternalInfo external,
		List<Yarn> yarns,
		List<Needle> needles,
		List<Gauge> gauges,
		List<Image> images) {

	public record Image(Long fileId, String url) {
	}

	public record ExternalInfo(String title, String creatorName) {
	}

	public record Yarn(String brand, String yarnName, String color, String amount, String unit, String note) {
	}

	public record Needle(String needleType, BigDecimal sizeMm, Integer lengthCm, String note) {
	}

	public record Gauge(BigDecimal stitches, BigDecimal rows, BigDecimal needleSizeMm, String measuredStage) {
	}
}
