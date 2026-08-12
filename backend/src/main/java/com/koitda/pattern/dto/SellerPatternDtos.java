package com.koitda.pattern.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.koitda.pattern.domain.CraftType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/** 판매자 도안 등록(SELLER-002·003, PATTERN-011) 요청·응답. */
public final class SellerPatternDtos {

	private SellerPatternDtos() {
	}

	/**
	 * 도안 임시저장/수정 요청. 사이즈·게이지는 자유 텍스트가 아닌 '행 단위 구조 입력'으로 받는다(SELLER-003).
	 * 스키마 위반(사이즈명·시작 콧수 누락 등)은 서비스에서 수동 검증해 422 로 응답한다.
	 * needle·technique 는 표시용 자유 구조 JSON 이라 JsonNode 로 그대로 통과시킨다.
	 */
	public record SavePatternDraftRequest(
			String title,
			String designerName,
			Long categoryId,
			CraftType craftType,
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
			JsonNode descriptionDocument,
			GaugeInput gauge,
			List<SizeInput> sizes,
			JsonNode needle,
			JsonNode technique,
			List<Long> imageFileIds,
			Long thumbnailFileId,
			Long pdfFileId) {
	}

	/** 게이지(PATTERN-011): 코 수·단 수·기준 크기·바늘 호수. 모두 양수여야 한다. */
	public record GaugeInput(
			Integer stitches,
			Integer rows,
			Double swatchWidthCm,
			Double swatchHeightCm,
			Double needleSizeMm) {
	}

	/** 사이즈 한 행(PATTERN-011): 사이즈명(label)·시작 콧수(castOnStitches)는 필수, 완성 실측은 항목별. */
	public record SizeInput(
			String label,
			Integer castOnStitches,
			Map<String, Double> measurements) {
	}

	/** 저장 결과. missingFields 는 제출 전 보완이 필요한 항목(이미지·PDF·가격 등) 안내. */
	public record DraftSavedResponse(Long draftId, String productStatus, List<String> missingFields) {
	}

	/** 내 도안 목록 한 줄(GET /seller/patterns). */
	public record SellerPatternListItem(
			Long id,
			String title,
			String productStatus,
			Long regularPrice,
			Long salePrice,
			String thumbnailUrl,
			OffsetDateTime updatedAt,
			OffsetDateTime publishedAt,
			String rejectionReason) {
	}

	/** 제출 결과. */
	public record SubmitResponse(Long patternId, String productStatus) {
	}

	/** 관리자 도안 심사 큐 한 줄(GET /admin/patterns). 판매자명 포함. */
	public record AdminPatternListItem(
			Long id,
			String title,
			String sellerBrand,
			String productStatus,
			Long regularPrice,
			Long salePrice,
			OffsetDateTime updatedAt) {
	}

	/** 관리자 심사 요청(승인 메모/반려 사유). */
	public record ReviewRequest(String reason) {
	}

	/**
	 * 판매자/관리자용 미리보기·상세. 승인 여부와 무관하게 초안 상태에서도 볼 수 있다.
	 * gauge/size/needle/technique 는 저장된 JSONB 원문을 그대로 노출한다.
	 */
	public record SellerPatternPreview(
			Long id,
			String title,
			String designerName,
			String sellerBrand,
			Long categoryId,
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
			@JsonRawValue String descriptionDocument,
			String productStatus,
			String rejectionReason,
			OffsetDateTime publishedAt,
			Long pdfFileId,
			List<Image> images,
			@JsonRawValue String gaugeInfo,
			@JsonRawValue String sizeInfo,
			@JsonRawValue String needleInfo,
			@JsonRawValue String techniqueInfo) {

		public record Image(String url, boolean thumbnail) {
		}
	}
}
