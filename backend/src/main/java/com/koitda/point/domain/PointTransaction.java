package com.koitda.point.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * 포인트 거래 원장(POINT-002). amount 는 부호 포함(적립 +, 회수 −).
 * balance_after 는 거래 직후 잔액 — 잔액이 어긋났을 때 어느 거래에서 틀어졌는지 추적한다.
 * idempotency_key(UNIQUE) 로 중복 적립·회수를 DB 레벨에서 차단한다.
 */
@Entity
@Table(name = "point_transaction")
public class PointTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "tx_type", nullable = false)
	private String txType;

	@Column(name = "amount", nullable = false)
	private int amount;

	@Column(name = "balance_after", nullable = false)
	private int balanceAfter;

	@Column(name = "reason_code")
	private String reasonCode;

	@Column(name = "review_id")
	private Long reviewId;

	@Column(name = "order_id")
	private Long orderId;

	@Column(name = "idempotency_key", nullable = false)
	private String idempotencyKey;

	@Column(name = "fail_reason")
	private String failReason;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	protected PointTransaction() {
	}

	/** 적립(EARN) — amount 는 양수. */
	public static PointTransaction earn(Long userId, int amount, int balanceAfter, String reasonCode,
			Long reviewId, String idempotencyKey) {
		return of(userId, "EARN", amount, balanceAfter, reasonCode, reviewId, idempotencyKey, null);
	}

	/** 회수(REVOKE) — amount 는 음수. */
	public static PointTransaction revoke(Long userId, int amount, int balanceAfter, String reasonCode,
			Long reviewId, String idempotencyKey) {
		return of(userId, "REVOKE", amount, balanceAfter, reasonCode, reviewId, idempotencyKey, null);
	}

	/** 회수 실패 기록 — 잔액 부족으로 회수하지 못했음을 amount 0 으로 남긴다(POINT-003). */
	public static PointTransaction revokeFailed(Long userId, int balanceAfter, String reasonCode,
			Long reviewId, String idempotencyKey, String failReason) {
		return of(userId, "REVOKE", 0, balanceAfter, reasonCode, reviewId, idempotencyKey, failReason);
	}

	private static PointTransaction of(Long userId, String txType, int amount, int balanceAfter,
			String reasonCode, Long reviewId, String idempotencyKey, String failReason) {
		PointTransaction t = new PointTransaction();
		t.userId = userId;
		t.txType = txType;
		t.amount = amount;
		t.balanceAfter = balanceAfter;
		t.reasonCode = reasonCode;
		t.reviewId = reviewId;
		t.idempotencyKey = idempotencyKey;
		t.failReason = failReason;
		t.createdAt = OffsetDateTime.now();
		return t;
	}

	public Long getId() {
		return id;
	}

	public String getTxType() {
		return txType;
	}

	public int getAmount() {
		return amount;
	}

	public int getBalanceAfter() {
		return balanceAfter;
	}

	public String getReasonCode() {
		return reasonCode;
	}

	public Long getReviewId() {
		return reviewId;
	}

	public String getFailReason() {
		return failReason;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
