package com.koitda.project.dto;

import com.koitda.project.dto.CreateProjectRequest.GaugeInput;
import com.koitda.project.dto.CreateProjectRequest.NeedleInput;
import com.koitda.project.dto.CreateProjectRequest.YarnInput;
import java.util.List;

/** 니팅로그 재료(실·바늘·게이지) 전체 교체. 등록 시와 동일한 입력 구조를 재사용한다. */
public record UpdateMaterialsRequest(
		List<YarnInput> yarns,
		List<NeedleInput> needles,
		List<GaugeInput> gauges) {

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
