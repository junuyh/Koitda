package com.koitda.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** 구매 도안(사용권). 환불 회수는 행 삭제가 아니라 revoked_at 기록(유출 추적 경로 보존). */
@Entity
@Table(name = "pattern_library")
public class PatternLibrary {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "pattern_id", nullable = false)
	private Long patternId;

	@Column(name = "order_item_id", nullable = false)
	private Long orderItemId;

	@Column(name = "purchased_at", nullable = false, updatable = false)
	private OffsetDateTime purchasedAt;

	@Column(name = "download_count", nullable = false)
	private int downloadCount;

	@Column(name = "download_limit", nullable = false)
	private int downloadLimit = 10;

	@Column(name = "revoked_at")
	private OffsetDateTime revokedAt;

	protected PatternLibrary() {
	}

	public static PatternLibrary create(Long userId, Long patternId, Long orderItemId) {
		PatternLibrary l = new PatternLibrary();
		l.userId = userId;
		l.patternId = patternId;
		l.orderItemId = orderItemId;
		l.purchasedAt = OffsetDateTime.now();
		return l;
	}

	/** 다운로드 1회 반영(LIBRARY). 한도를 넘으면 막는다. */
	public void recordDownload() {
		if (downloadCount >= downloadLimit) {
			throw new IllegalStateException("다운로드 한도를 초과했습니다.");
		}
		this.downloadCount++;
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public Long getId() {
		return id;
	}

	public Long getPatternId() {
		return patternId;
	}

	public Long getUserId() {
		return userId;
	}

	public OffsetDateTime getPurchasedAt() {
		return purchasedAt;
	}

	public int getDownloadCount() {
		return downloadCount;
	}

	public int getDownloadLimit() {
		return downloadLimit;
	}
}

