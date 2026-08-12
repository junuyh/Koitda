package com.koitda.pattern.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 도안 상세(PATTERN-004). gauge/size/needle/technique 는 JSONB 원문을 그대로 중첩 JSON 으로 노출한다.
 * 보유 여부·리뷰 작성 가능 여부는 주문·리뷰 도메인 도입 시 추가한다.
 */
public record PatternDetailResponse(
		Long id,
		String title,
		String designerName,
		String sellerBrand,
		Long categoryId,
		String categoryName,
		String craftType,
		String difficulty,
		String language,
		Long regularPrice,
		Long salePrice,
		String productForm,
		String deliveryMethod,
		Integer availabilityDays,
		String referenceVideoUrl,
		Integer pageCount,
		String yarnRequirement,
		String description,
		OffsetDateTime publishedAt,
		int viewCount,
		int reviewCount,
		int wishCount,
		int publicProjectCount,
		boolean wished,
		List<Image> images,
		@JsonRawValue String gaugeInfo,
		@JsonRawValue String sizeInfo,
		@JsonRawValue String needleInfo,
		@JsonRawValue String techniqueInfo) {

	public record Image(String url, boolean thumbnail) {
	}
}
