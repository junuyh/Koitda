package com.koitda.pattern.api;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.pattern.domain.ProductStatus;
import com.koitda.pattern.dto.SellerPatternDtos.AdminPatternListItem;
import com.koitda.pattern.dto.SellerPatternDtos.ReviewRequest;
import com.koitda.pattern.dto.SellerPatternDtos.SellerPatternPreview;
import com.koitda.pattern.service.SellerPatternService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 도안 심사(ADMIN-001). 경로 /api/v1/admin/** 는 SecurityConfig 에서 ROLE_ADMIN 요구. */
@RestController
@RequestMapping("/api/v1/admin/patterns")
public class AdminPatternController {

	private final SellerPatternService service;

	public AdminPatternController(SellerPatternService service) {
		this.service = service;
	}

	/** 심사 큐. status 미지정 시 전체 상태(기본 심사 대상은 PENDING). */
	@GetMapping
	public List<AdminPatternListItem> list(@RequestParam(required = false) ProductStatus status) {
		return service.adminList(status);
	}

	/** 심사 상세 — 상태 무관. */
	@GetMapping("/{patternId}")
	public SellerPatternPreview detail(@PathVariable Long patternId) {
		return service.adminDetail(patternId);
	}

	@PostMapping("/{patternId}/approve")
	public void approve(@PathVariable Long patternId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		service.approve(principal.getUserId(), patternId);
	}

	@PostMapping("/{patternId}/reject")
	public void reject(@PathVariable Long patternId, @RequestBody(required = false) ReviewRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		service.reject(principal.getUserId(), patternId, request != null ? request.reason() : null);
	}
}
