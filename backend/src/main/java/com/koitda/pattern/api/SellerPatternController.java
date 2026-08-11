package com.koitda.pattern.api;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.pattern.domain.ProductStatus;
import com.koitda.pattern.dto.SellerPatternDtos.DraftSavedResponse;
import com.koitda.pattern.dto.SellerPatternDtos.SavePatternDraftRequest;
import com.koitda.pattern.dto.SellerPatternDtos.SellerPatternListItem;
import com.koitda.pattern.dto.SellerPatternDtos.SellerPatternPreview;
import com.koitda.pattern.dto.SellerPatternDtos.SubmitResponse;
import com.koitda.pattern.service.SellerPatternService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 판매자 도안 등록(SELLER-002·003). 경로 /api/v1/seller/** 는 SecurityConfig 에서 ROLE_SELLER 요구.
 * 초안(pattern-drafts)과 상품(patterns)은 같은 엔티티이며 상태로만 구분한다.
 */
@RestController
public class SellerPatternController {

	private final SellerPatternService service;

	public SellerPatternController(SellerPatternService service) {
		this.service = service;
	}

	/** 도안 임시저장 — 새 초안 생성. */
	@PostMapping("/api/v1/seller/pattern-drafts")
	@ResponseStatus(HttpStatus.CREATED)
	public DraftSavedResponse createDraft(@RequestBody SavePatternDraftRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return service.createDraft(principal.getUserId(), request);
	}

	/** 초안 수정. */
	@PatchMapping("/api/v1/seller/pattern-drafts/{draftId}")
	public DraftSavedResponse updateDraft(@PathVariable Long draftId,
			@RequestBody SavePatternDraftRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return service.updateDraft(principal.getUserId(), draftId, request);
	}

	/** 도안 미리보기 — 소유자만, 상태 무관. */
	@GetMapping("/api/v1/seller/pattern-drafts/{draftId}/preview")
	public SellerPatternPreview preview(@PathVariable Long draftId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return service.preview(principal.getUserId(), draftId);
	}

	/** 도안 제출 — DRAFT·REJECTED → PENDING. */
	@PostMapping("/api/v1/seller/pattern-drafts/{draftId}/submit")
	public SubmitResponse submit(@PathVariable Long draftId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return service.submit(principal.getUserId(), draftId);
	}

	/** 내 도안 목록. status 미지정 시 전체 상태. */
	@GetMapping("/api/v1/seller/patterns")
	public List<SellerPatternListItem> myPatterns(@RequestParam(required = false) ProductStatus status,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return service.myPatterns(principal.getUserId(), status);
	}
}
