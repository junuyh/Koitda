package com.koitda.seller.api;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.seller.dto.SellerDtos.RejectRequest;
import com.koitda.seller.service.SellerService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 전용(ADMIN-001). 경로 /api/v1/admin/** 는 SecurityConfig 에서 ROLE_ADMIN 요구. */
@RestController
@RequestMapping("/api/v1/admin/seller-applications")
public class AdminSellerController {

	private final SellerService sellerService;

	public AdminSellerController(SellerService sellerService) {
		this.sellerService = sellerService;
	}

	@PostMapping("/{applicationId}/approve")
	public void approve(@PathVariable Long applicationId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		sellerService.approve(principal.getUserId(), applicationId);
	}

	@PostMapping("/{applicationId}/reject")
	public void reject(@PathVariable Long applicationId, @RequestBody(required = false) RejectRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		sellerService.reject(principal.getUserId(), applicationId, request != null ? request.reason() : null);
	}
}
