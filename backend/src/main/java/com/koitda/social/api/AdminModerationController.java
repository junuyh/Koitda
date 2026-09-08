package com.koitda.social.api;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.social.dto.ModerationDtos.ReportedItem;
import com.koitda.social.service.AdminModerationService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 신고 관리(ADMIN-002). /api/v1/admin/** 는 SecurityConfig 에서 ROLE_ADMIN 요구. */
@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminModerationController {

	private final AdminModerationService service;

	public AdminModerationController(AdminModerationService service) {
		this.service = service;
	}

	/** 신고 큐(기본 미처리 PENDING). resolution=HIDDEN|DISMISSED|RESTORED 로 이력 조회. */
	@GetMapping
	public List<ReportedItem> list(@RequestParam(required = false) String resolution) {
		return service.list(resolution);
	}

	@PostMapping("/{moderationId}/hide")
	public void hide(@PathVariable Long moderationId, @AuthenticationPrincipal CustomUserDetails principal) {
		service.hide(moderationId, principal.getUserId());
	}

	@PostMapping("/{moderationId}/dismiss")
	public void dismiss(@PathVariable Long moderationId, @AuthenticationPrincipal CustomUserDetails principal) {
		service.dismiss(moderationId, principal.getUserId());
	}

	@PostMapping("/{moderationId}/restore")
	public void restore(@PathVariable Long moderationId, @AuthenticationPrincipal CustomUserDetails principal) {
		service.restore(moderationId, principal.getUserId());
	}
}
