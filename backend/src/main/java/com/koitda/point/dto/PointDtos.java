package com.koitda.point.dto;

import com.koitda.point.domain.PointTransaction;
import java.time.OffsetDateTime;
import java.util.List;

public final class PointDtos {

	private PointDtos() {
	}

	/** 포인트 내역 한 줄(POINT-006). */
	public record PointTxItem(
			Long id,
			String txType,
			int amount,
			int balanceAfter,
			String reasonCode,
			Long reviewId,
			String failReason,
			OffsetDateTime createdAt) {

		public static PointTxItem from(PointTransaction t) {
			return new PointTxItem(t.getId(), t.getTxType(), t.getAmount(), t.getBalanceAfter(),
					t.getReasonCode(), t.getReviewId(), t.getFailReason(), t.getCreatedAt());
		}
	}

	/** 잔액 + 내역. */
	public record PointHistoryResponse(int balance, List<PointTxItem> transactions) {
	}
}
