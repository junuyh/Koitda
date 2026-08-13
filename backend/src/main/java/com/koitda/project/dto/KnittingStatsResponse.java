package com.koitda.project.dto;

import java.util.List;

/**
 * 도안별 니팅로그 집계(코잇다 핵심 가치) — 다른 사람들이 이 도안을 어떤 실·바늘·게이지로 떴는지.
 * 구매 결정을 돕는 실제 제작 정보다.
 */
public record KnittingStatsResponse(
		long projectCount,
		long finishedCount,
		List<YarnStat> yarns,
		List<NeedleStat> needles,
		List<GaugeStat> gauges) {

	public record YarnStat(String label, long count) {
	}

	public record NeedleStat(String sizeMm, long count) {
	}

	public record GaugeStat(String stitches, String rows, long count) {
	}
}
