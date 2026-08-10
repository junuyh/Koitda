package com.koitda.pattern.service;

import com.koitda.common.dto.PageResponse;
import com.koitda.pattern.domain.CraftType;
import com.koitda.pattern.domain.SellingPattern;
import com.koitda.pattern.domain.SellingPatternImage;
import com.koitda.pattern.dto.PatternListItemResponse;
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

	public PatternQueryService(SellingPatternRepository patternRepository,
			SellingPatternImageRepository imageRepository, WishRepository wishRepository) {
		this.patternRepository = patternRepository;
		this.imageRepository = imageRepository;
		this.wishRepository = wishRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<PatternListItemResponse> search(String q, Long categoryId, CraftType craftType,
			String difficulty, Long minPrice, Long maxPrice, Long userId, Pageable pageable) {
		String query = (q == null || q.isBlank()) ? null : q.trim();
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
