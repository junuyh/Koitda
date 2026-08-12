package com.koitda.pattern.service;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.file.repository.FileAssetRepository;
import com.koitda.pattern.domain.ProductStatus;
import com.koitda.pattern.domain.SellingPattern;
import com.koitda.pattern.domain.SellingPatternImage;
import com.koitda.pattern.dto.SellerPatternDtos.AdminPatternListItem;
import com.koitda.pattern.dto.SellerPatternDtos.DraftSavedResponse;
import com.koitda.pattern.dto.SellerPatternDtos.GaugeInput;
import com.koitda.pattern.dto.SellerPatternDtos.SavePatternDraftRequest;
import com.koitda.pattern.dto.SellerPatternDtos.SellerPatternListItem;
import com.koitda.pattern.dto.SellerPatternDtos.SellerPatternPreview;
import com.koitda.pattern.dto.SellerPatternDtos.SizeInput;
import com.koitda.pattern.dto.SellerPatternDtos.SubmitResponse;
import com.koitda.pattern.repository.PatternCategoryRepository;
import com.koitda.pattern.repository.SellingPatternImageRepository;
import com.koitda.pattern.repository.SellingPatternRepository;
import com.koitda.seller.domain.SellerProfile;
import com.koitda.seller.repository.SellerProfileRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * 판매자 도안 등록·심사 흐름(SELLER-002·003, PATTERN-011, ADMIN-001).
 * 초안과 상품은 같은 selling_pattern 테이블이며 product_status 로 구분한다(draftId == patternId).
 * 사이즈·게이지는 구조를 강하게 검증해 저장 시점에 항상 유효한 JSONB 만 남긴다 → 제출 시엔 존재만 확인.
 */
@Service
public class SellerPatternService {

	private final SellingPatternRepository patternRepository;
	private final SellingPatternImageRepository imageRepository;
	private final SellerProfileRepository sellerProfileRepository;
	private final PatternCategoryRepository categoryRepository;
	private final FileAssetRepository fileAssetRepository;
	private final ObjectMapper objectMapper;

	public SellerPatternService(SellingPatternRepository patternRepository,
			SellingPatternImageRepository imageRepository, SellerProfileRepository sellerProfileRepository,
			PatternCategoryRepository categoryRepository, FileAssetRepository fileAssetRepository,
			ObjectMapper objectMapper) {
		this.patternRepository = patternRepository;
		this.imageRepository = imageRepository;
		this.sellerProfileRepository = sellerProfileRepository;
		this.categoryRepository = categoryRepository;
		this.fileAssetRepository = fileAssetRepository;
		this.objectMapper = objectMapper;
	}

	// ---------------------------------------------------------------- 판매자

	/** 도안 임시저장(SELLER-002) — 새 초안 생성. */
	@Transactional
	public DraftSavedResponse createDraft(Long userId, SavePatternDraftRequest req) {
		SellerProfile seller = requireSeller(userId);
		validateStructure(req);
		validateReferences(req);

		SellingPattern draft = SellingPattern.createDraft(seller);
		applyDetails(draft, req);
		patternRepository.save(draft);        // id 확보(이미지 FK 용)
		applyImages(draft.getId(), req);

		return new DraftSavedResponse(draft.getId(), draft.getProductStatus().name(), missingFields(req));
	}

	/** 초안 수정(SELLER-003) — DRAFT·REJECTED 상태만. */
	@Transactional
	public DraftSavedResponse updateDraft(Long userId, Long draftId, SavePatternDraftRequest req) {
		SellingPattern draft = requireOwned(userId, draftId);
		validateStructure(req);
		validateReferences(req);

		applyDetails(draft, req);             // editDetails 가 DRAFT·REJECTED 아니면 IllegalState
		applyImages(draft.getId(), req);

		return new DraftSavedResponse(draft.getId(), draft.getProductStatus().name(), missingFields(req));
	}

	/** 미리보기(SELLER-002) — 소유자만, 상태 무관. */
	@Transactional(readOnly = true)
	public SellerPatternPreview preview(Long userId, Long patternId) {
		return toPreview(requireOwned(userId, patternId));
	}

	/** preview 응답 조립(판매자 미리보기·관리자 심사 공용). */
	private SellerPatternPreview toPreview(SellingPattern p) {
		List<SellerPatternPreview.Image> images = imageRepository.findByPattern(p.getId()).stream()
				.map(i -> new SellerPatternPreview.Image(
						i.getFile() != null ? PatternQueryService.fileUrl(i.getFile().getId()) : null, i.isThumbnail()))
				.toList();
		return new SellerPatternPreview(
				p.getId(), p.getTitle(), p.getDesignerName(),
				p.getSeller() != null ? p.getSeller().getBrandName() : null,
				p.getCategoryId(),
				p.getCraftType() != null ? p.getCraftType().name() : null,
				p.getDifficulty(), p.getLanguage(), p.getRegularPrice(), p.getSalePrice(),
				p.getProductForm(), p.getDeliveryMethod(), p.getAvailabilityDays(),
				p.getReferenceVideoUrl(), p.getPageCount(), p.getYarnRequirement(), p.getDescription(),
				p.getDescriptionDocument(),
				p.getProductStatus().name(), p.getRejectionReason(), p.getPublishedAt(), p.getCurrentFileId(),
				images, p.getGaugeInfo(), p.getSizeInfo(), p.getNeedleInfo(), p.getTechniqueInfo());
	}

	/** 내 도안 목록(GET /seller/patterns). status 문자열은 null 이면 전체. */
	@Transactional(readOnly = true)
	public List<SellerPatternListItem> myPatterns(Long userId, ProductStatus status) {
		SellerProfile seller = requireSeller(userId);
		List<SellingPattern> patterns = patternRepository.findMine(seller.getId(), status);
		if (patterns.isEmpty()) {
			return List.of();
		}
		Map<Long, String> thumbnails = new LinkedHashMap<>();
		List<Long> ids = patterns.stream().map(SellingPattern::getId).toList();
		for (SellingPatternImage image : imageRepository.findThumbnails(ids)) {
			thumbnails.putIfAbsent(image.getPatternId(),
					image.getFile() != null ? PatternQueryService.fileUrl(image.getFile().getId()) : null);
		}
		return patterns.stream().map(p -> new SellerPatternListItem(
				p.getId(), p.getTitle(), p.getProductStatus().name(),
				p.getRegularPrice(), p.getSalePrice(), thumbnails.get(p.getId()),
				p.getUpdatedAt(), p.getPublishedAt(), p.getRejectionReason())).toList();
	}

	/** 심사 제출(SELLER-002): DRAFT·REJECTED → PENDING. 필수 항목이 없으면 제출을 막는다. */
	@Transactional
	public SubmitResponse submit(Long userId, Long draftId) {
		SellingPattern p = requireOwned(userId, draftId);
		requireSubmittable(p);
		try {
			p.submit();
		} catch (IllegalStateException e) {
			throw new ApiException(ErrorCode.INVALID_PATTERN_STATE, "제출할 수 없는 상태입니다.");
		}
		return new SubmitResponse(p.getId(), p.getProductStatus().name());
	}

	// ---------------------------------------------------------------- 관리자

	/** 도안 심사 큐(GET /admin/patterns). status 가 null 이면 전체. */
	@Transactional(readOnly = true)
	public List<AdminPatternListItem> adminList(ProductStatus status) {
		return patternRepository.findForReview(status).stream()
				.map(p -> new AdminPatternListItem(
						p.getId(), p.getTitle(),
						p.getSeller() != null ? p.getSeller().getBrandName() : null,
						p.getProductStatus().name(), p.getRegularPrice(), p.getSalePrice(), p.getUpdatedAt()))
				.toList();
	}

	/** 도안 심사 상세(GET /admin/patterns/{id}) — 상태 무관, 소유 제한 없음. */
	@Transactional(readOnly = true)
	public SellerPatternPreview adminDetail(Long patternId) {
		SellingPattern p = patternRepository.findWithSellerById(patternId)
				.orElseThrow(() -> new ApiException(ErrorCode.PATTERN_NOT_FOUND, "도안을 찾을 수 없습니다."));
		return toPreview(p);
	}

	/** 도안 승인(ADMIN-001): PENDING → APPROVED. */
	@Transactional
	public void approve(Long reviewerId, Long patternId) {
		SellingPattern p = patternRepository.findById(patternId)
				.orElseThrow(() -> new ApiException(ErrorCode.PATTERN_NOT_FOUND, "도안을 찾을 수 없습니다."));
		try {
			p.approve(reviewerId);
		} catch (IllegalStateException e) {
			throw new ApiException(ErrorCode.INVALID_PATTERN_STATE, "심사 대기(PENDING) 도안만 승인할 수 있습니다.");
		}
	}

	/** 도안 반려(ADMIN-001): PENDING → REJECTED. 사유 저장. */
	@Transactional
	public void reject(Long reviewerId, Long patternId, String reason) {
		SellingPattern p = patternRepository.findById(patternId)
				.orElseThrow(() -> new ApiException(ErrorCode.PATTERN_NOT_FOUND, "도안을 찾을 수 없습니다."));
		try {
			p.reject(reviewerId, reason);
		} catch (IllegalStateException e) {
			throw new ApiException(ErrorCode.INVALID_PATTERN_STATE, "심사 대기(PENDING) 도안만 반려할 수 있습니다.");
		}
	}

	// ---------------------------------------------------------------- 내부

	private SellerProfile requireSeller(Long userId) {
		return sellerProfileRepository.findByUserId(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED, "판매자만 접근할 수 있습니다."));
	}

	/** 소유자 검증 — 소유자가 아니면 존재를 숨기려 404. */
	private SellingPattern requireOwned(Long userId, Long patternId) {
		SellerProfile seller = requireSeller(userId);
		return patternRepository.findByIdAndSeller_Id(patternId, seller.getId())
				.orElseThrow(() -> new ApiException(ErrorCode.PATTERN_NOT_FOUND, "도안을 찾을 수 없습니다."));
	}

	private void applyDetails(SellingPattern p, SavePatternDraftRequest req) {
		String gaugeJson = req.gauge() == null ? null : objectMapper.writeValueAsString(req.gauge());
		String sizeJson = req.sizes() == null ? null
				: objectMapper.writeValueAsString(Map.of("sizes", req.sizes()));
		String needleJson = req.needle() == null ? null : objectMapper.writeValueAsString(req.needle());
		String techniqueJson = req.technique() == null ? null : objectMapper.writeValueAsString(req.technique());
		String descDocJson = req.descriptionDocument() == null ? null
				: objectMapper.writeValueAsString(req.descriptionDocument());
		try {
			p.editDetails(
					blankToNull(req.title()), blankToNull(req.designerName()), req.categoryId(), req.craftType(),
					blankToNull(req.difficulty()), blankToNull(req.language()), req.regularPrice(), req.salePrice(),
					blankToNull(req.productForm()), blankToNull(req.deliveryMethod()), req.availabilityDays(),
					blankToNull(req.referenceVideoUrl()), req.pageCount(), blankToNull(req.yarnRequirement()),
					blankToNull(req.description()), descDocJson, gaugeJson, sizeJson, needleJson, techniqueJson,
					req.pdfFileId());
		} catch (IllegalStateException e) {
			throw new ApiException(ErrorCode.INVALID_PATTERN_STATE, "DRAFT·REJECTED 상태에서만 수정할 수 있습니다.");
		}
	}

	/** 이미지 전체 교체. thumbnailFileId 미지정 시 첫 이미지를 대표로 삼는다. */
	private void applyImages(Long patternId, SavePatternDraftRequest req) {
		if (req.imageFileIds() == null) {
			return; // 이미지 항목 자체를 안 보낸 부분 저장 — 기존 이미지 유지
		}
		imageRepository.deleteByPatternId(patternId);
		imageRepository.flush();
		List<Long> ids = req.imageFileIds();
		List<SellingPatternImage> rows = new ArrayList<>();
		for (int i = 0; i < ids.size(); i++) {
			Long fileId = ids.get(i);
			boolean thumbnail = req.thumbnailFileId() != null
					? fileId.equals(req.thumbnailFileId())
					: i == 0;
			rows.add(SellingPatternImage.create(patternId, fileAssetRepository.getReferenceById(fileId), i, thumbnail));
		}
		imageRepository.saveAll(rows);
	}

	// --- 구조 검증(사이즈·게이지) : 위반 시 422 PATTERN_SIZE_SCHEMA_INVALID ---

	private void validateStructure(SavePatternDraftRequest req) {
		if (req.gauge() != null) {
			validateGauge(req.gauge());
		}
		if (req.sizes() != null) {
			validateSizes(req.sizes());
		}
	}

	private void validateGauge(GaugeInput g) {
		if (!positive(g.stitches()) || !positive(g.rows())
				|| !positive(g.swatchWidthCm()) || !positive(g.swatchHeightCm()) || !positive(g.needleSizeMm())) {
			throw new ApiException(ErrorCode.PATTERN_SIZE_SCHEMA_INVALID,
					"게이지는 코 수·단 수·기준 크기·바늘 호수를 모두 양수로 입력해야 합니다.");
		}
	}

	private void validateSizes(List<SizeInput> sizes) {
		if (sizes.isEmpty()) {
			throw new ApiException(ErrorCode.PATTERN_SIZE_SCHEMA_INVALID, "사이즈를 한 개 이상 입력해야 합니다.");
		}
		for (SizeInput s : sizes) {
			if (s == null || s.label() == null || s.label().isBlank()) {
				throw new ApiException(ErrorCode.PATTERN_SIZE_SCHEMA_INVALID, "사이즈명은 필수입니다.");
			}
			if (!positive(s.castOnStitches())) {
				throw new ApiException(ErrorCode.PATTERN_SIZE_SCHEMA_INVALID,
						"'" + s.label() + "' 사이즈의 시작 콧수는 양의 정수여야 합니다.");
			}
			if (s.measurements() != null) {
				for (Double v : s.measurements().values()) {
					if (v == null || v < 0) {
						throw new ApiException(ErrorCode.PATTERN_SIZE_SCHEMA_INVALID, "완성 실측은 0 이상이어야 합니다.");
					}
				}
			}
		}
	}

	private void validateReferences(SavePatternDraftRequest req) {
		if (req.categoryId() != null && !categoryRepository.existsById(req.categoryId())) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "존재하지 않는 카테고리입니다.");
		}
		if (req.imageFileIds() != null) {
			for (Long fileId : req.imageFileIds()) {
				if (fileId == null || !fileAssetRepository.existsById(fileId)) {
					throw new ApiException(ErrorCode.VALIDATION_ERROR, "존재하지 않는 이미지 파일입니다.");
				}
			}
			if (req.thumbnailFileId() != null && !req.imageFileIds().contains(req.thumbnailFileId())) {
				throw new ApiException(ErrorCode.VALIDATION_ERROR, "대표 이미지는 이미지 목록 안에 있어야 합니다.");
			}
		}
		if (req.pdfFileId() != null && !fileAssetRepository.existsById(req.pdfFileId())) {
			throw new ApiException(ErrorCode.VALIDATION_ERROR, "존재하지 않는 PDF 파일입니다.");
		}
	}

	/** 제출 가능 여부. 사이즈·게이지 누락은 422, 그 외 필수(제목·방식·가격) 누락은 400. */
	private void requireSubmittable(SellingPattern p) {
		if (p.getGaugeInfo() == null || p.getSizeInfo() == null) {
			throw new ApiException(ErrorCode.PATTERN_SIZE_SCHEMA_INVALID,
					"제출하려면 게이지와 사이즈를 모두 입력해야 합니다.");
		}
		List<String> missing = new ArrayList<>();
		if (p.getTitle() == null) {
			missing.add("title");
		}
		if (p.getCraftType() == null) {
			missing.add("craftType");
		}
		if (p.getRegularPrice() == null) {
			missing.add("regularPrice");
		}
		if (!missing.isEmpty()) {
			throw new ApiException(ErrorCode.PATTERN_INCOMPLETE, "필수 항목이 비어 있습니다: " + String.join(", ", missing));
		}
	}

	/** 제출 전 보완이 필요한 항목(차단은 아니고 안내). */
	private List<String> missingFields(SavePatternDraftRequest req) {
		List<String> missing = new ArrayList<>();
		if (blankToNull(req.title()) == null) {
			missing.add("title");
		}
		if (req.craftType() == null) {
			missing.add("craftType");
		}
		if (req.regularPrice() == null) {
			missing.add("regularPrice");
		}
		if (req.gauge() == null) {
			missing.add("gauge");
		}
		if (req.sizes() == null || req.sizes().isEmpty()) {
			missing.add("sizes");
		}
		if (req.imageFileIds() == null || req.imageFileIds().isEmpty()) {
			missing.add("images");
		}
		if (req.pdfFileId() == null) {
			missing.add("pdf");
		}
		return missing;
	}

	private static boolean positive(Integer v) {
		return v != null && v > 0;
	}

	private static boolean positive(Double v) {
		return v != null && v > 0;
	}

	private static String blankToNull(String s) {
		return (s == null || s.isBlank()) ? null : s.trim();
	}
}
