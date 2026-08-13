package com.koitda.project.api;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.project.dto.CreateProjectRequest;
import com.koitda.project.dto.ProjectCreatedResponse;
import com.koitda.project.dto.ProjectDetailResponse;
import com.koitda.project.dto.ChangeVisibilityRequest;
import com.koitda.project.dto.ProjectGroupResponse;
import com.koitda.project.dto.ProjectListItemResponse;
import com.koitda.project.dto.ProjectTrashDtos.TrashItemResponse;
import com.koitda.project.dto.ProjectTrashDtos.TrashResult;
import com.koitda.project.dto.VisibilityImpactResponse;
import com.koitda.project.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

	/** 대표 이미지 추가(PROJECT-010). 소유자만. body: {fileId}. */
	@PostMapping("/{projectId}/images")
	public ProjectDetailResponse addImage(@PathVariable Long projectId,
			@RequestBody com.koitda.project.dto.AddImageRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return projectService.addImage(principal.getUserId(), projectId, request.fileId());
	}

	/** 대표 이미지 삭제. 소유자만. */
	@DeleteMapping("/{projectId}/images/{fileId}")
	public ProjectDetailResponse removeImage(@PathVariable Long projectId, @PathVariable Long fileId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return projectService.removeImage(principal.getUserId(), projectId, fileId);
	}

	/** 공개 니팅로그 피드(둘러보기, PROJECT-피드). 비로그인도 조회 가능. sort=likes|recent. */
	@GetMapping("/feed")
	public List<com.koitda.project.dto.ProjectFeedItemResponse> feed(
			@org.springframework.web.bind.annotation.RequestParam(defaultValue = "recent") String sort,
			@org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
			@org.springframework.web.bind.annotation.RequestParam(defaultValue = "12") int size) {
		return projectService.publicFeed(sort, page, size);
	}

	/** 휴지통 목록(DATA-003). */
	@GetMapping("/trash")
	public List<TrashItemResponse> trash(@AuthenticationPrincipal CustomUserDetails principal) {
		return projectService.trashList(principal.getUserId());
	}

	/** 휴지통 이동(PROJECT-016) — 연결 로그도 함께. */
	@DeleteMapping("/{projectId}")
	public TrashResult moveToTrash(@PathVariable Long projectId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return projectService.moveToTrash(principal.getUserId(), projectId);
	}

	/** 복구. */
	@PostMapping("/{projectId}/restore")
	public void restore(@PathVariable Long projectId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		projectService.restore(principal.getUserId(), projectId);
	}

	/** 완전 삭제. */
	@DeleteMapping("/{projectId}/permanent")
	public void permanentDelete(@PathVariable Long projectId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		projectService.permanentDelete(principal.getUserId(), projectId);
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

	/** 공개 변경 영향 조회(PROJECT-019) — 비공개 전환 전 함께 비공개될 공개 로그 수. */
	@PostMapping("/{projectId}/visibility-impact")
	public VisibilityImpactResponse visibilityImpact(@PathVariable Long projectId,
			@Valid @RequestBody ChangeVisibilityRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return projectService.visibilityImpact(principal.getUserId(), projectId, request.visibility());
	}

	/** 니팅로그 공개 설정 변경(PROJECT-019). 비공개 전환 시 하위 로그 함께 비공개. */
	@PatchMapping("/{projectId}/visibility")
	public void changeVisibility(@PathVariable Long projectId,
			@Valid @RequestBody ChangeVisibilityRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		projectService.changeVisibility(principal.getUserId(), projectId, request.visibility(), request.confirmed());
	}
}
