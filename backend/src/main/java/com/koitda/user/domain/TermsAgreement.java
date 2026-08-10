package com.koitda.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** 약관 동의 이력(AUTH-006). 종류·버전·동의 시각을 함께 보존한다. */
@Entity
@Table(name = "terms_agreement")
public class TermsAgreement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "terms_type", nullable = false)
	private TermsType termsType;

	@Column(name = "terms_version", nullable = false)
	private String termsVersion;

	@Column(name = "is_agreed", nullable = false)
	private boolean agreed;

	@Column(name = "agreed_at", nullable = false)
	private OffsetDateTime agreedAt;

	protected TermsAgreement() {
	}

	public TermsAgreement(Long userId, TermsType termsType, String termsVersion, boolean agreed) {
		this.userId = userId;
		this.termsType = termsType;
		this.termsVersion = termsVersion;
		this.agreed = agreed;
	}

	@PrePersist
	void onCreate() {
		this.agreedAt = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public TermsType getTermsType() {
		return termsType;
	}

	public boolean isAgreed() {
		return agreed;
	}
}
