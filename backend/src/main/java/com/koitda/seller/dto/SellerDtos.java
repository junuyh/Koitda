package com.koitda.seller.dto;

import com.koitda.seller.domain.BusinessType;
import jakarta.validation.constraints.NotBlank;

public final class SellerDtos {

	private SellerDtos() {
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
