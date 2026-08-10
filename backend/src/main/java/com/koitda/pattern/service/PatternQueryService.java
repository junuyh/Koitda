package com.koitda.pattern.service;

import com.koitda.common.dto.PageResponse;
import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.pattern.domain.CraftType;
import com.koitda.pattern.domain.PatternCategory;
import com.koitda.pattern.domain.ProductStatus;
import com.koitda.pattern.domain.SellingPattern;
import com.koitda.pattern.domain.SellingPatternImage;
import com.koitda.pattern.domain.Wish;
import com.koitda.pattern.dto.PatternDetailResponse;
import com.koitda.pattern.dto.PatternListItemResponse;
import com.koitda.pattern.repository.PatternCategoryRepository;
import com.koitda.pattern.repository.SellingPatternImageRepository;
import com.koitda.pattern.repository.SellingPatternRepository;
import com.koitda.pattern.repository.WishRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatternQueryService {

	private final SellingPatternRepository patternRepository;
	private final SellingPatternImageRepository imageRepository;
	private final WishRepository wishRepository;
	private final PatternCategoryRepository categoryRepository;

	public PatternQueryService(SellingPatternRepository patternRepository,
			SellingPatternImageRepository imageRepository, WishRepository wishRepository,
			PatternCategoryRepository categoryRepository) {
		this.patternRepository = patternRepository;
		this.imageRepository = imageRepository;
		this.wishRepository = wishRepository;
		this.categoryRepository = categoryRepository;
	}

	@Transactional(readOnly = true)
	public PatternDetailResponse detail(Long patternId, Long userId) {
		SellingPattern p = patternRepository.findByIdAndProductStatus(patternId, ProductStatus.APPROVED)
				.orElseThrow(() -> new ApiException(ErrorCode.PATTERN_NOT_FOUND, "도안을 찾을 수 없습니다."));

		String categoryName = (p.getCategoryId() == null) ? null
				: categoryRepository.findById(p.getCategoryId())
						.map(PatternCategory::getName).orElse(null);

		List<PatternDetailResponse.Image> images = imageRepository.findByPattern(patternId).stream()
				.map(i -> new PatternDetailResponse.Image(
						i.getFile() != null ? i.getFile().getStorageKey() : null, i.isThumbnail()))
				.toList();

		boolean wished = userId != null && wishRepository.existsById(new Wish.WishId(userId, patternId));

		return new PatternDetailResponse(
				p.getId(), p.getTitle(), p.getDesignerName(),
				p.getSeller() != null ? p.getSeller().getBrandName() : null,
				p.getCategoryId(), categoryName,
				p.getCraftType() != null ? p.getCraftType().name() : null,
				p.getDifficulty(), p.getLanguage(),
				p.getRegularPrice(), p.getSalePrice(),
				p.getProductForm(), p.getDeliveryMethod(), p.getAvailabilityDays(),
				p.getReferenceVideoUrl(), p.getPageCount(),
				p.getYarnRequirement(), p.getDescription(), p.getPublishedAt(),
				p.getViewCount(), p.getReviewCount(), p.getWishCount(), p.getPublicProjectCount(), wished,
				images,
				p.getGaugeInfo(), p.getSizeInfo(), p.getNeedleInfo(), p.getTechniqueInfo());
	}

	@Transactional(readOnly = true)
	public PageResponse<PatternListItemResponse> search(String q, Long categoryId, CraftType craftType,
			String difficulty, Long minPrice, Long maxPrice, Long userId, Pageable pageable) {
		// 빈 검색어는 null 이 아니라 "" 로 넘긴다. null 이면 PostgreSQL 이
		// lower(concat('%', :q, '%')) 의 파라미터 타입을 bytea 로 추론해 오류가 난다.
		String query = (q == null || q.isBlank()) ? "" : q.trim();
		Page<SellingPattern> page = patternRepository.search(
				query, categoryId, craftType, difficulty, minPrice, maxPrice, pageable);
		return PageResponse.of(page, assemble(page.getContent(), userId));
	}

	@Transactional(readOnly = true)
	public PageResponse<PatternListItemResponse> listWished(Long userId, Pageable pageable) {
		Page<SellingPattern> page = wishRepository.findWishedPatterns(userId, pageable);
		return PageResponse.of(page, assemble(page.getContent(), userId));
	}

	/** 목록에 대표이미지·위시여부를 각 1회 쿼리로 채워 N+1 을 피한다. */
	private List<PatternListItemResponse> assemble(List<SellingPattern> patterns, Long userId) {
		if (patterns.isEmpty()) {
			return List.of();
		}
		List<Long> ids = patterns.stream().map(SellingPattern::getId).toList();

		Map<Long, String> thumbnails = new HashMap<>();
		for (SellingPatternImage image : imageRepository.findThumbnails(ids)) {
			thumbnails.putIfAbsent(image.getPatternId(), thumbnailKey(image));
		}

		Set<Long> wished = (userId == null)
				? Set.of()
				: new HashSet<>(wishRepository.findWishedPatternIds(userId, ids));

		return patterns.stream().map(p -> new PatternListItemResponse(
				p.getId(),
				p.getTitle(),
				p.getDesignerName(),
				p.getSeller() != null ? p.getSeller().getBrandName() : null,
				p.getSalePrice(),
				p.getRegularPrice(),
				p.getDifficulty(),
				p.getCraftType() != null ? p.getCraftType().name() : null,
				thumbnails.get(p.getId()),
				p.getWishCount(),
				p.getPublicProjectCount(),
				wished.contains(p.getId()))).toList();
	}

	private String thumbnailKey(SellingPatternImage image) {
		if (image.getFile() == null) {
			return null;
		}
		return image.getFile().getThumbnailKey() != null
				? image.getFile().getThumbnailKey()
				: image.getFile().getStorageKey();
	}
}
