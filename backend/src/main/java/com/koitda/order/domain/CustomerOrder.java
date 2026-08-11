package com.koitda.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/** 주문(ORDER-006: 도안 1건 단위지만 주문·항목 구조는 확장 가능하게 유지). */
@Entity
@Table(name = "customer_order")
public class CustomerOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "order_no", nullable = false)
	private String orderNo;

	@Column(name = "buyer_id", nullable = false)
	private Long buyerId;

	@Column(name = "total_amount", nullable = false)
	private long totalAmount;

	@Column(name = "used_point", nullable = false)
	private int usedPoint;

	@Column(name = "payment_amount", nullable = false)
	private long paymentAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "order_status", nullable = false)
	private OrderStatus orderStatus = OrderStatus.CREATED;

	@Column(name = "ordered_at", nullable = false, updatable = false)
	private OffsetDateTime orderedAt;

	@Column(name = "paid_at")
	private OffsetDateTime paidAt;

	protected CustomerOrder() {
	}

	public static CustomerOrder create(String orderNo, Long buyerId, long totalAmount, int usedPoint) {
		CustomerOrder o = new CustomerOrder();
		o.orderNo = orderNo;
		o.buyerId = buyerId;
		o.totalAmount = totalAmount;
		o.usedPoint = usedPoint;
		o.paymentAmount = totalAmount - usedPoint;
		o.orderStatus = OrderStatus.CREATED;
		o.orderedAt = OffsetDateTime.now();
		return o;
	}

	public void markPaid() {
		this.orderStatus = OrderStatus.PAID;
		this.paidAt = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public Long getBuyerId() {
		return buyerId;
	}

	public long getTotalAmount() {
		return totalAmount;
	}

	public long getPaymentAmount() {
		return paymentAmount;
	}

	public OrderStatus getOrderStatus() {
		return orderStatus;
	}

	public OffsetDateTime getOrderedAt() {
		return orderedAt;
	}
}
