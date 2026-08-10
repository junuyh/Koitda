package com.koitda.pattern.domain;

import com.koitda.seller.domain.SellerProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 도안 본체. 이번 슬라이스는 카탈로그 조회용 스칼라 필드만 매핑한다.
 * gauge_info·size_info 등 JSONB 는 상세 화면 슬라이스에서 추가한다(미매핑 컬럼은 조회에 영향 없음).
 */
@Entity
@Table(name = "selling_pattern")
public class SellingPattern {

	@Id
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

	protected SellingPattern() {
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
