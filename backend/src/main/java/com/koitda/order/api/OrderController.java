package com.koitda.order.api;

import com.koitda.common.security.CustomUserDetails;
import com.koitda.order.dto.OrderDtos.CreateOrderRequest;
import com.koitda.order.dto.OrderDtos.DemoPaymentResponse;
import com.koitda.order.dto.OrderDtos.LibraryItemResponse;
import com.koitda.order.dto.OrderDtos.OrderListItemResponse;
import com.koitda.order.dto.OrderDtos.OrderResponse;
import com.koitda.order.dto.OrderDtos.PurchasabilityResponse;
import com.koitda.order.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	/** 구매 가능 확인(ORDER-005). 로그인 필요. */
	@GetMapping("/patterns/{patternId}/purchasability")
	public PurchasabilityResponse purchasability(@PathVariable Long patternId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return orderService.purchasability(principal.getUserId(), patternId);
	}

	/** 주문 생성(ORDER-001·006). 보유 도안이면 ALREADY_OWNED. */
	@PostMapping("/orders")
	@ResponseStatus(HttpStatus.CREATED)
	public OrderResponse create(@Valid @RequestBody CreateOrderRequest request,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return orderService.createOrder(principal.getUserId(), request);
	}

	/** 데모 결제 완료 → 구매 도안 생성. */
	@PostMapping("/orders/{orderId}/payments/complete")
	public DemoPaymentResponse completePayment(@PathVariable Long orderId,
			@AuthenticationPrincipal CustomUserDetails principal) {
		return orderService.completeDemoPayment(principal.getUserId(), orderId);
	}

	/** 내 주문 목록. */
	@GetMapping("/users/me/orders")
	public List<OrderListItemResponse> myOrders(@AuthenticationPrincipal CustomUserDetails principal) {
		return orderService.myOrders(principal.getUserId());
	}

	/** 구매 도안 목록(LIBRARY-001). */
	@GetMapping("/users/me/pattern-library")
	public List<LibraryItemResponse> myLibrary(@AuthenticationPrincipal CustomUserDetails principal) {
		return orderService.myLibrary(principal.getUserId());
	}
}
