package com.koitda.project.api;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.project.dto.CreateProjectRequest;
import com.koitda.project.dto.ProjectCreatedResponse;
import com.koitda.project.dto.ProjectDetailResponse;
import com.koitda.project.dto.ProjectGroupResponse;
import com.koitda.project.dto.ProjectListItemResponse;
import com.koitda.project.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	/** 니팅로그 생성(PROJECT-003~012). 로그인 필요. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProjectCreatedResponse create(@Valid @RequestBody CreateProjectRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return projectService.create(principal.getUserId(), request);
	}

	/** 내 니팅로그 플랫 목록. */
	@GetMapping("/mine")
	public List<ProjectListItemResponse> mine(@AuthenticationPrincipal CustomUserDetails principal) {
		return projectService.myProjects(principal.getUserId());
	}

	/** 코잇기 — 도안 타래(연결 도안별 그룹) 목록(PROJECT-001·002·013). */
	@GetMapping("/grouped-by-pattern")
	public List<ProjectGroupResponse> grouped(@AuthenticationPrincipal CustomUserDetails principal) {
		return projectService.groupedByPattern(principal.getUserId());
	}

	/** 니팅로그 상세(PROJECT-011·012). 소유자 또는 공개 니팅로그. */
	@GetMapping("/{projectId}")
	public ProjectDetailResponse detail(@PathVariable Long projectId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return projectService.detail(projectId, principal.getUserId());
	}
}
