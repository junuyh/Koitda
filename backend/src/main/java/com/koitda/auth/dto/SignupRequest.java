package com.koitda.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SignupRequest(
		@NotBlank @Email String email,
		// BCrypt 는 72바이트까지만 유효하므로 상한을 명시한다.
		@NotBlank @Size(min = 8, max = 72) String password,
		@NotBlank @Size(max = 30) String nickname,
		@NotEmpty @Valid List<TermsAgreementRequest> agreements) {
}
