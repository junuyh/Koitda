package com.koitda.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** 주문 항목. 가격·수수료율을 주문 시점 값으로 복사(과거 정산 불변). */
@Entity
@Table(name = "order_item")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_id", nullable = false)
	private Long orderId;

	@Column(name = "pattern_id", nullable = false)
	private Long patternId;

	@Column(name = "seller_id", nullable = false)
	private Long sellerId;

	@Column(name = "pattern_title_snapshot")
	private String patternTitleSnapshot;

	@Column(name = "unit_price", nullable = false)
	private long unitPrice;

	@Column(name = "item_amount", nullable = false)
	private long itemAmount;

	@Column(name = "platform_fee_rate")
	private BigDecimal platformFeeRate;

	@Column(name = "platform_fee_amount")
	private Long platformFeeAmount;

	@Column(name = "pg_fee_rate")
	private BigDecimal pgFeeRate;

	@Column(name = "pg_fee_amount")
	private Long pgFeeAmount;

	@Column(name = "refund_amount", nullable = false)
	private long refundAmount;

	@Column(name = "settlement_amount")
	private Long settlementAmount;

	protected OrderItem() {
	}

	public static OrderItem create(Long orderId, Long patternId, Long sellerId, String titleSnapshot,
			long unitPrice, BigDecimal platformFeeRate) {
		OrderItem i = new OrderItem();
		i.orderId = orderId;
		i.patternId = patternId;
		i.sellerId = sellerId;
		i.patternTitleSnapshot = titleSnapshot;
		i.unitPrice = unitPrice;
		i.itemAmount = unitPrice;
		i.platformFeeRate = platformFeeRate;
		// 수수료 반올림은 버림(floor). 정산액 = 단가 − 플랫폼수수료 − PG수수료 − 환불
		i.platformFeeAmount = unitPrice * platformFeeRate.longValue() / 100;
		i.pgFeeRate = BigDecimal.ZERO;   // 데모: PG 수수료 없음
		i.pgFeeAmount = 0L;
		i.refundAmount = 0L;
		i.settlementAmount = unitPrice - i.platformFeeAmount - i.pgFeeAmount;
		return i;
	}

	public Long getId() {
		return id;
	}

	public Long getPatternId() {
		return patternId;
	}
}
