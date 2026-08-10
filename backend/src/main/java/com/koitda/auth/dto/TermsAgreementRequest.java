package com.koitda.auth.dto;

import com.koitda.user.domain.TermsType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TermsAgreementRequest(
		@NotNull TermsType termsType,
		@NotBlank String termsVersion,
		boolean agreed) {
}
