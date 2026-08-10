package com.koitda.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** 외부 도안(사용자 소유). 유사 이름 자동 병합 안 함. PDF·본문 이미지는 저장하지 않는다. */
@Entity
@Table(name = "external_pattern")
public class ExternalPattern {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "creator_name")
	private String creatorName;

	@Column(name = "purchase_place")
	private String purchasePlace;

	@Column(name = "purchase_url")
	private String purchaseUrl;

	@Column(name = "purchase_price")
	private Long purchasePrice;

	@Column(name = "purchase_date")
	private LocalDate purchaseDate;

	@Column(name = "memo")
	private String memo;

	@Column(name = "craft_type")
	private String craftType;

	@Column(name = "category_id")
	private Long categoryId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected ExternalPattern() {
	}

	public ExternalPattern(Long userId, String title, String creatorName, String purchasePlace,
			String purchaseUrl, Long purchasePrice, LocalDate purchaseDate, String memo,
			String craftType, Long categoryId) {
		this.userId = userId;
		this.title = title;
		this.creatorName = creatorName;
		this.purchasePlace = purchasePlace;
		this.purchaseUrl = purchaseUrl;
		this.purchasePrice = purchasePrice;
		this.purchaseDate = purchaseDate;
		this.memo = memo;
		this.craftType = craftType;
		this.categoryId = categoryId;
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

	public String getTitle() {
		return title;
	}

	public String getCreatorName() {
		return creatorName;
	}
}
