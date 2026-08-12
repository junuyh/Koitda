package com.koitda.pattern.api;

import com.koitda.common.dto.PageResponse;
import com.koitda.common.security.CustomUserDetails;
import com.koitda.pattern.domain.CraftType;
import com.koitda.pattern.dto.PatternDetailResponse;
import com.koitda.pattern.dto.PatternListItemResponse;
import com.koitda.pattern.service.PatternQueryService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patterns")
public class PatternController {

	private final PatternQueryService patternQueryService;

	public PatternController(PatternQueryService patternQueryService) {
		this.patternQueryService = patternQueryService;
	}

	/** 도안 목록(PATTERN-001·002·003). 비로그인도 조회 가능하며, 로그인 시 위시 여부를 함께 준다. */
	@GetMapping
	public PageResponse<PatternListItemResponse> list(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Long categoryId,
			@RequestParam(required = false) CraftType craftType,
			@RequestParam(required = false) String difficulty,
			@RequestParam(required = false) Long minPrice,
			@RequestParam(required = false) Long maxPrice,
			@AuthenticationPrincipal CustomUserDetails principal,
			@PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
		Long userId = (principal != null) ? principal.getUserId() : null;
		return patternQueryService.search(q, categoryId, craftType, difficulty, minPrice, maxPrice, userId, pageable);
	}

	/** 베스트셀러 도안(홈 가로 스크롤). 구매수 기준 상위 N개. 비로그인도 조회 가능. */
	@GetMapping("/best-sellers")
	public java.util.List<PatternListItemResponse> bestSellers(
			@RequestParam(defaultValue = "12") int limit,
			@AuthenticationPrincipal CustomUserDetails principal) {
		Long userId = (principal != null) ? principal.getUserId() : null;
		return patternQueryService.bestSellers(userId, Math.min(Math.max(limit, 1), 30));
	}

	/** 도안 상세(PATTERN-004). 비로그인도 조회 가능. */
	@GetMapping("/{patternId}")
	public PatternDetailResponse detail(
			@org.springframework.web.bind.annotation.PathVariable Long patternId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		Long userId = (principal != null) ? principal.getUserId() : null;
		return patternQueryService.detail(patternId, userId);
	}
}
