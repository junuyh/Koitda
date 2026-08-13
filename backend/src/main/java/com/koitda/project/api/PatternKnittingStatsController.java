package com.koitda.project.api;

import com.koitda.project.dto.KnittingStatsResponse;
import com.koitda.project.service.ProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 도안별 니팅로그 집계(실·바늘·게이지 실사용). 구매 결정을 돕는 공개 정보. */
@RestController
public class PatternKnittingStatsController {

	private final ProjectService projectService;

	public PatternKnittingStatsController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping("/api/v1/patterns/{patternId}/knitting-stats")
	public KnittingStatsResponse stats(@PathVariable Long patternId) {
		return projectService.knittingStats(patternId);
	}
}
