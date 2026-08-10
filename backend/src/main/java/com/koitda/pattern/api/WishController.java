package com.koitda.pattern.api;

import com.koitda.common.dto.PageResponse;
import com.koitda.common.security.CustomUserDetails;
import com.koitda.pattern.dto.PatternListItemResponse;
import com.koitda.pattern.dto.WishResponse;
import com.koitda.pattern.service.PatternQueryService;
import com.koitda.pattern.service.WishService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class WishController {

	private final WishService wishService;
	private final PatternQueryService patternQueryService;

	public WishController(WishService wishService, PatternQueryService patternQueryService) {
		this.wishService = wishService;
		this.patternQueryService = patternQueryService;
	}

	/** 위시 등록(PATTERN-009). 로그인 필요. */
	@PostMapping("/patterns/{patternId}/wish")
	public WishResponse add(@PathVariable Long patternId, @AuthenticationPrincipal CustomUserDetails principal) {
		return wishService.add(principal.getUserId(), patternId);
	}

	/** 위시 해제. */
	@DeleteMapping("/patterns/{patternId}/wish")
	public WishResponse remove(@PathVariable Long patternId, @AuthenticationPrincipal CustomUserDetails principal) {
		return wishService.remove(principal.getUserId(), patternId);
	}

	/** 내 위시 목록. */
	@GetMapping("/users/me/wishes")
	public PageResponse<PatternListItemResponse> myWishes(
			@AuthenticationPrincipal CustomUserDetails principal,
			@PageableDefault(size = 20) Pageable pageable) {
		return patternQueryService.listWished(principal.getUserId(), pageable);
	}
}
