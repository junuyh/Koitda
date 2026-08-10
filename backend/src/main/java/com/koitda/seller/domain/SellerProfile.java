package com.koitda.seller.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 판매자. 도안의 판매 주체이며 목록·상세에서 판매자명(brand)을 보여준다. */
@Entity
@Table(name = "seller_profile")
public class SellerProfile {

	@Id
	private Long id;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "brand_name")
	private String brandName;

	@Column(name = "status")
	private String status;

	protected SellerProfile() {
	}

	public Long getId() {
		return id;
	}

	public String getBrandName() {
		return brandName;
	}
}
