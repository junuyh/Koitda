package com.koitda.order.dto;

import com.koitda.order.repository.LibraryView;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** 주문·구매 도안 관련 요청·응답 DTO 모음. */
public final class OrderDtos {

	private OrderDtos() {
	}

	public record CreateOrderRequest(@NotNull Long patternId, Integer usedPoint, boolean agreed) {
	}

	public record OrderResponse(Long id, String orderNo, long totalAmount, long paymentAmount, String status) {
	}

	public record PurchasabilityResponse(boolean canPurchase, String reason, int usablePoint) {
	}

	public record DemoPaymentResponse(String orderStatus, Long patternId) {
	}

	public record OrderListItemResponse(Long id, String orderNo, long totalAmount, long paymentAmount,
			String status, OffsetDateTime orderedAt) {
	}

	public record LibraryItemResponse(Long patternId, String patternTitle, String categoryName,
			OffsetDateTime purchasedAt, boolean revoked) {

		public static LibraryItemResponse from(LibraryView v) {
			return new LibraryItemResponse(v.getPatternId(), v.getPatternTitle(), v.getCategoryName(),
					v.getPurchasedAt().atOffset(ZoneOffset.UTC), v.getRevoked());
		}
	}

	/** 구매 도안 상세(LIBRARY) — 다운로드 화면용. */
	public record LibraryDetailResponse(
			Long patternId,
			String patternTitle,
			String sellerBrand,
			OffsetDateTime purchasedAt,
			boolean revoked,
			boolean hasPdf,
			int downloadCount,
			int downloadLimit) {
	}
}
