package com.koitda.pattern.domain;

import com.koitda.seller.domain.SellerProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 도안 본체. 카탈로그 조회와 판매자 등록(SELLER-002·003) 양쪽에서 쓴다.
 * product_status 전이: DRAFT → PENDING(제출) → APPROVED/REJECTED(관리자 심사).
 * gauge_info·size_info 등 JSONB 는 원시 JSON 문자열로 매핑(@JdbcTypeCode).
 */
@Entity
@Table(name = "selling_pattern")
public class SellingPattern {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seller_id")
	private SellerProfile seller;

	@Column(name = "title")
	private String title;

	@Column(name = "designer_name")
	private String designerName;

	@Column(name = "category_id")
	private Long categoryId;

	@Enumerated(EnumType.STRING)
	@Column(name = "craft_type")
	private CraftType craftType;

	@Column(name = "difficulty")
	private String difficulty;

	@Column(name = "language")
	private String language;

	@Column(name = "regular_price")
	private Long regularPrice;

	@Column(name = "sale_price")
	private Long salePrice;

	@Column(name = "page_count")
	private Integer pageCount;

	@Enumerated(EnumType.STRING)
	@Column(name = "product_status")
	private ProductStatus productStatus;

	@Column(name = "wish_count")
	private int wishCount;

	@Column(name = "public_project_count")
	private int publicProjectCount;

	@Column(name = "review_count")
	private int reviewCount;

	@Column(name = "view_count")
	private int viewCount;

	@Column(name = "published_at")
	private OffsetDateTime publishedAt;

	// --- 상세 화면용 추가 필드 ---
	@Column(name = "product_form")
	private String productForm;

	@Column(name = "delivery_method")
	private String deliveryMethod;

	@Column(name = "availability_days")
	private Integer availabilityDays;

	@Column(name = "reference_video_url")
	private String referenceVideoUrl;

	@Column(name = "yarn_requirement")
	private String yarnRequirement;

	@Column(name = "description")
	private String description;

	// JSONB → 원시 JSON 문자열로 매핑(@JdbcTypeCode). 응답에서는 @JsonRawValue 로 중첩 JSON 그대로 노출.
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "gauge_info")
	private String gaugeInfo;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "size_info")
	private String sizeInfo;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "needle_info")
	private String needleInfo;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "technique_info")
	private String techniqueInfo;

	@Column(name = "current_file_id")
	private Long currentFileId;

	// --- 심사(승인/반려) 감사. 관리자만 갱신(ADMIN-001) ---
	@Column(name = "reviewed_by")
	private Long reviewedBy;

	@Column(name = "reviewed_at")
	private OffsetDateTime reviewedAt;

	@Column(name = "rejection_reason")
	private String rejectionReason;

	@Column(name = "created_at", updatable = false, insertable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;

	protected SellingPattern() {
	}

	/** 판매자 도안 초안 생성(SELLER-002). 상태는 DRAFT 로 시작한다. */
	public static SellingPattern createDraft(SellerProfile seller) {
		SellingPattern p = new SellingPattern();
		p.seller = seller;
		p.productStatus = ProductStatus.DRAFT;
		p.updatedAt = OffsetDateTime.now();
		return p;
	}

	/**
	 * 초안 상세를 갱신한다(SELLER-003). DRAFT·REJECTED 상태에서만 편집 가능.
	 * JSON 4종은 서비스에서 검증·직렬화한 문자열을 그대로 받는다.
	 */
	public void editDetails(String title, String designerName, Long categoryId, CraftType craftType,
			String difficulty, String language, Long regularPrice, Long salePrice, String productForm,
			String deliveryMethod, Integer availabilityDays, String referenceVideoUrl, Integer pageCount,
			String yarnRequirement, String description, String gaugeInfo, String sizeInfo, String needleInfo,
			String techniqueInfo, Long currentFileId) {
		requireEditable();
		this.title = title;
		this.designerName = designerName;
		this.categoryId = categoryId;
		this.craftType = craftType;
		this.difficulty = difficulty;
		this.language = language;
		this.regularPrice = regularPrice;
		this.salePrice = salePrice;
		this.productForm = productForm;
		this.deliveryMethod = deliveryMethod;
		this.availabilityDays = availabilityDays;
		this.referenceVideoUrl = referenceVideoUrl;
		this.pageCount = pageCount;
		this.yarnRequirement = yarnRequirement;
		this.description = description;
		this.gaugeInfo = gaugeInfo;
		this.sizeInfo = sizeInfo;
		this.needleInfo = needleInfo;
		this.techniqueInfo = techniqueInfo;
		this.currentFileId = currentFileId;
		this.updatedAt = OffsetDateTime.now();
	}

	/** 심사 제출(SELLER-002): DRAFT·REJECTED → PENDING. 반려 사유는 재제출 시 초기화. */
	public void submit() {
		requireEditable();
		this.productStatus = ProductStatus.PENDING;
		this.rejectionReason = null;
		this.updatedAt = OffsetDateTime.now();
	}

	/** 관리자 승인(ADMIN-001): PENDING → APPROVED. 최초 승인 시 published_at 기록. */
	public void approve(Long reviewerId) {
		requireStatus(ProductStatus.PENDING);
		this.productStatus = ProductStatus.APPROVED;
		this.reviewedBy = reviewerId;
		this.reviewedAt = OffsetDateTime.now();
		this.rejectionReason = null;
		this.updatedAt = OffsetDateTime.now();
		if (this.publishedAt == null) {
			this.publishedAt = OffsetDateTime.now();
		}
	}

	/** 관리자 반려(ADMIN-001): PENDING → REJECTED. 사유 저장. */
	public void reject(Long reviewerId, String reason) {
		requireStatus(ProductStatus.PENDING);
		this.productStatus = ProductStatus.REJECTED;
		this.reviewedBy = reviewerId;
		this.reviewedAt = OffsetDateTime.now();
		this.rejectionReason = reason;
		this.updatedAt = OffsetDateTime.now();
	}

	private void requireEditable() {
		if (productStatus != ProductStatus.DRAFT && productStatus != ProductStatus.REJECTED) {
			throw new IllegalStateException("DRAFT·REJECTED 상태에서만 편집·제출할 수 있습니다: " + productStatus);
		}
	}

	private void requireStatus(ProductStatus expected) {
		if (productStatus != expected) {
			throw new IllegalStateException("기대 상태 " + expected + " 가 아닙니다: " + productStatus);
		}
	}

	public ProductStatus getProductStatus() {
		return productStatus;
	}

	public Long getCurrentFileId() {
		return currentFileId;
	}

	public Long getReviewedBy() {
		return reviewedBy;
	}

	public OffsetDateTime getReviewedAt() {
		return reviewedAt;
	}

	public String getRejectionReason() {
		return rejectionReason;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public Long getId() {
		return id;
	}

	public SellerProfile getSeller() {
		return seller;
	}

	public String getTitle() {
		return title;
	}

	public String getDesignerName() {
		return designerName;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public CraftType getCraftType() {
		return craftType;
	}

	public String getDifficulty() {
		return difficulty;
	}

	public String getLanguage() {
		return language;
	}

	public Long getRegularPrice() {
		return regularPrice;
	}

	public Long getSalePrice() {
		return salePrice;
	}

	public Integer getPageCount() {
		return pageCount;
	}

	public int getWishCount() {
		return wishCount;
	}

	public int getPublicProjectCount() {
		return publicProjectCount;
	}

	public int getReviewCount() {
		return reviewCount;
	}

	public int getViewCount() {
		return viewCount;
	}

	public OffsetDateTime getPublishedAt() {
		return publishedAt;
	}

	public String getProductForm() {
		return productForm;
	}

	public String getDeliveryMethod() {
		return deliveryMethod;
	}

	public Integer getAvailabilityDays() {
		return availabilityDays;
	}

	public String getReferenceVideoUrl() {
		return referenceVideoUrl;
	}

	public String getYarnRequirement() {
		return yarnRequirement;
	}

	public String getDescription() {
		return description;
	}

	public String getGaugeInfo() {
		return gaugeInfo;
	}

	public String getSizeInfo() {
		return sizeInfo;
	}

	public String getNeedleInfo() {
		return needleInfo;
	}

	public String getTechniqueInfo() {
		return techniqueInfo;
	}
}
