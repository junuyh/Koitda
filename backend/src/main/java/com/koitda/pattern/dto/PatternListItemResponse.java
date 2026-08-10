package com.koitda.pattern.dto;

/** 도안 목록 카드(PATTERN-003). 카테고리는 카드에 표시하지 않는다. */
public record PatternListItemResponse(
		Long id,
		String title,
		String designerName,
		String sellerBrand,
		Long salePrice,
		Long regularPrice,
		String difficulty,
		String craftType,
		String thumbnailKey,
		int wishCount,
		int publicProjectCount,
		boolean wished) {
}
