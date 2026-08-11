package com.koitda.seller.dto;

import com.koitda.seller.domain.BusinessType;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

public final class SellerDtos {

	private SellerDtos() {
	}

	/** 관리자 판매자 심사 큐 한 줄(GET /admin/seller-applications). 정산계좌는 마스킹해 노출. */
	public record AdminApplicationItem(
			Long id,
			String brandName,
			BusinessType businessType,
			String businessNo,
			String representativeName,
			String settlementBank,
			String settlementAccountMasked,
			String status,
			String rejectionReason,
			OffsetDateTime createdAt,
			OffsetDateTime reviewedAt) {
	}

	public record SellerApplicationRequest(
			@NotBlank String brandName,
			BusinessType businessType,
			String businessNo,
			String representativeName,
			String settlementBank,
			String settlementAccount,
			String termsVersion) {
	}

	public record SellerApplicationResponse(Long id, String status) {
	}

	public record RejectRequest(String reason) {
	}
}
