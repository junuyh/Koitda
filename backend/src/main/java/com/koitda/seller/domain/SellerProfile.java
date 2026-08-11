package com.koitda.seller.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** 판매자. 승인 시 신청서로부터 생성된다. platform_fee_rate 는 DB 기본값(10.00) 사용. */
@Entity
@Table(name = "seller_profile")
public class SellerProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "brand_name", nullable = false)
	private String brandName;

	@Column(name = "status", nullable = false)
	private String status = "ACTIVE";

	@Column(name = "approved_at")
	private OffsetDateTime approvedAt;

	protected SellerProfile() {
	}

	public static SellerProfile create(Long userId, String brandName) {
		SellerProfile p = new SellerProfile();
		p.userId = userId;
		p.brandName = brandName;
		p.status = "ACTIVE";
		p.approvedAt = OffsetDateTime.now();
		return p;
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
}
