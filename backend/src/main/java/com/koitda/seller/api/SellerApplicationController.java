package com.koitda.seller.api;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.seller.dto.SellerDtos.SellerApplicationRequest;
import com.koitda.seller.dto.SellerDtos.SellerApplicationResponse;
import com.koitda.seller.service.SellerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seller-applications")
public class SellerApplicationController {

	private final SellerService sellerService;

	public SellerApplicationController(SellerService sellerService) {
		this.sellerService = sellerService;
	}

	/** 판매자 신청(SELLER-001). 로그인 필요. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SellerApplicationResponse apply(@Valid @RequestBody SellerApplicationRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return sellerService.apply(principal.getUserId(), request);
	}
}
