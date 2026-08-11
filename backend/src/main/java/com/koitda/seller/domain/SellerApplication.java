package com.koitda.seller.domain;

import com.koitda.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** 판매자 신청(SELLER-001). 사업자번호·정산계좌는 컬럼 암호화 저장. */
@Entity
@Table(name = "seller_application")
public class SellerApplication {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "brand_name", nullable = false)
	private String brandName;

	@Enumerated(EnumType.STRING)
	@Column(name = "business_type")
	private BusinessType businessType;

	@Convert(converter = EncryptedStringConverter.class)
	@Column(name = "business_no_enc")
	private String businessNo;

	@Column(name = "representative_name")
	private String representativeName;

	@Column(name = "settlement_bank")
	private String settlementBank;

	@Convert(converter = EncryptedStringConverter.class)
	@Column(name = "settlement_account_enc")
	private String settlementAccount;

	@Column(name = "terms_version")
	private String termsVersion;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private ApplicationStatus status = ApplicationStatus.PENDING;

	@Column(name = "reviewed_by")
	private Long reviewedBy;

	@Column(name = "reviewed_at")
	private OffsetDateTime reviewedAt;

	@Column(name = "rejection_reason")
	private String rejectionReason;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected SellerApplication() {
	}

	public static SellerApplication create(Long userId, String brandName, BusinessType businessType,
			String businessNo, String representativeName, String settlementBank, String settlementAccount,
			String termsVersion) {
		SellerApplication a = new SellerApplication();
		a.userId = userId;
		a.brandName = brandName;
		a.businessType = businessType;
		a.businessNo = businessNo;
		a.representativeName = representativeName;
		a.settlementBank = settlementBank;
		a.settlementAccount = settlementAccount;
		a.termsVersion = termsVersion;
		a.status = ApplicationStatus.PENDING;
		return a;
	}

	public void approve(Long reviewerId) {
		this.status = ApplicationStatus.APPROVED;
		this.reviewedBy = reviewerId;
		this.reviewedAt = OffsetDateTime.now();
	}

	public void reject(Long reviewerId, String reason) {
		this.status = ApplicationStatus.REJECTED;
		this.reviewedBy = reviewerId;
		this.reviewedAt = OffsetDateTime.now();
		this.rejectionReason = reason;
	}

	@PrePersist
	void onCreate() {
		this.createdAt = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public String getBrandName() {
		return brandName;
	}

	public ApplicationStatus getStatus() {
		return status;
	}

	public BusinessType getBusinessType() {
		return businessType;
	}

	public String getBusinessNo() {
		return businessNo;
	}

	public String getRepresentativeName() {
		return representativeName;
	}

	public String getSettlementBank() {
		return settlementBank;
	}

	public String getSettlementAccount() {
		return settlementAccount;
	}

	public String getRejectionReason() {
		return rejectionReason;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getReviewedAt() {
		return reviewedAt;
	}
}
