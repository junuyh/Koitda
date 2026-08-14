package com.koitda.inquiry.api;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.inquiry.dto.InquiryDtos.AnswerInquiryRequest;
import com.koitda.inquiry.dto.InquiryDtos.CreateInquiryRequest;
import com.koitda.inquiry.dto.InquiryDtos.InquiryCreatedResponse;
import com.koitda.inquiry.dto.InquiryDtos.InquiryListResponse;
import com.koitda.inquiry.dto.InquiryDtos.SellerInboxResponse;
import com.koitda.inquiry.dto.InquiryDtos.UpdateInquiryRequest;
import com.koitda.inquiry.service.InquiryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 도안 문의(문의하기). 목록은 공개(비공개 문의는 마스킹), 작성·수정·삭제·답변은 로그인 필요.
 * 판매자 인박스(/seller/inquiries)는 SELLER 역할이 필요(SecurityConfig /seller/** 매처).
 */
@RestController
public class InquiryController {

	private final InquiryService inquiryService;

	public InquiryController(InquiryService inquiryService) {
		this.inquiryService = inquiryService;
	}

	/** 도안 상세 문의 목록. 비로그인 허용. */
	@GetMapping("/api/v1/patterns/{patternId}/inquiries")
	public InquiryListResponse list(@PathVariable Long patternId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		Long viewerId = principal != null ? principal.getUserId() : null;
		return inquiryService.list(patternId, viewerId, isAdmin(principal));
	}

	/** 문의 작성. 로그인 필요. */
	@PostMapping("/api/v1/patterns/{patternId}/inquiries")
	@ResponseStatus(HttpStatus.CREATED)
	public InquiryCreatedResponse create(@PathVariable Long patternId,
			@RequestBody CreateInquiryRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return inquiryService.create(principal.getUserId(), patternId, request);
	}

	/** 판매자 답변. 해당 도안의 판매자만(서비스에서 검증). */
	@PostMapping("/api/v1/inquiries/{inquiryId}/answer")
	public void answer(@PathVariable Long inquiryId, @RequestBody AnswerInquiryRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		inquiryService.answer(inquiryId, principal.getUserId(), request);
	}

	/** 작성자 문의 수정(미답변일 때만). */
	@PatchMapping("/api/v1/inquiries/{inquiryId}")
	public void update(@PathVariable Long inquiryId, @RequestBody UpdateInquiryRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		inquiryService.update(inquiryId, principal.getUserId(), request);
	}

	/** 문의 삭제(작성자 또는 관리자). */
	@DeleteMapping("/api/v1/inquiries/{inquiryId}")
	public void delete(@PathVariable Long inquiryId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		inquiryService.delete(inquiryId, principal.getUserId(), isAdmin(principal));
	}

	/** 판매자 인박스(미답변 = 알림). unanswered=true 면 미답변만. */
	@GetMapping("/api/v1/seller/inquiries")
	public SellerInboxResponse sellerInbox(
			@RequestParam(name = "unanswered", defaultValue = "false") boolean onlyUnanswered,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return inquiryService.sellerInbox(principal.getUserId(), onlyUnanswered);
	}

	private boolean isAdmin(CustomUserDetails principal) {
		if (principal == null) {
			return false;
		}
		for (GrantedAuthority a : principal.getAuthorities()) {
			if ("ROLE_ADMIN".equals(a.getAuthority())) {
				return true;
			}
		}
		return false;
	}
}
