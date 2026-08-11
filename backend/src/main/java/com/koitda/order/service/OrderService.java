package com.koitda.order.service;

import com.koitda.common.error.ApiException;
import com.koitda.common.error.ErrorCode;
import com.koitda.order.domain.CustomerOrder;
import com.koitda.order.domain.OrderItem;
import com.koitda.order.domain.OrderStatus;
import com.koitda.order.domain.PatternLibrary;
import com.koitda.order.dto.OrderDtos.CreateOrderRequest;
import com.koitda.order.dto.OrderDtos.DemoPaymentResponse;
import com.koitda.order.dto.OrderDtos.LibraryItemResponse;
import com.koitda.order.dto.OrderDtos.OrderListItemResponse;
import com.koitda.order.dto.OrderDtos.OrderResponse;
import com.koitda.order.dto.OrderDtos.PurchasabilityResponse;
import com.koitda.order.repository.CustomerOrderRepository;
import com.koitda.order.repository.OrderItemRepository;
import com.koitda.order.repository.PatternLibraryRepository;
import com.koitda.pattern.domain.ProductStatus;
import com.koitda.pattern.domain.SellingPattern;
import com.koitda.pattern.repository.SellingPatternRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

	private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("10.00"); // SELLER-005 초기 10%

	private final CustomerOrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final PatternLibraryRepository libraryRepository;
	private final SellingPatternRepository patternRepository;

	public OrderService(CustomerOrderRepository orderRepository, OrderItemRepository orderItemRepository,
			PatternLibraryRepository libraryRepository, SellingPatternRepository patternRepository) {
		this.orderRepository = orderRepository;
		this.orderItemRepository = orderItemRepository;
		this.libraryRepository = libraryRepository;
		this.patternRepository = patternRepository;
	}

	@Transactional(readOnly = true)
	public PurchasabilityResponse purchasability(Long userId, Long patternId) {
		boolean approved = patternRepository.findByIdAndProductStatus(patternId, ProductStatus.APPROVED).isPresent();
		if (!approved) {
			return new PurchasabilityResponse(false, "판매 중인 도안이 아닙니다.", 0);
		}
		if (libraryRepository.existsByUserIdAndPatternIdAndRevokedAtIsNull(userId, patternId)) {
			return new PurchasabilityResponse(false, "이미 보유한 도안입니다.", 0);
		}
		return new PurchasabilityResponse(true, null, 0);
	}

	/** 주문 생성(ORDER-006). 보유 도안이면 서버가 차단(ORDER-005). */
	@Transactional
	public OrderResponse createOrder(Long userId, CreateOrderRequest req) {
		SellingPattern pattern = patternRepository
				.findByIdAndProductStatus(req.patternId(), ProductStatus.APPROVED)
				.orElseThrow(() -> new ApiException(ErrorCode.PATTERN_NOT_FOUND, "판매 중인 도안이 아닙니다."));
		if (libraryRepository.existsByUserIdAndPatternIdAndRevokedAtIsNull(userId, pattern.getId())) {
			throw new ApiException(ErrorCode.ALREADY_OWNED, "이미 보유한 도안입니다.");
		}

		long price = pattern.getSalePrice() != null ? pattern.getSalePrice() : 0L;
		String orderNo = "ORD-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
		CustomerOrder order = orderRepository.save(CustomerOrder.create(orderNo, userId, price, 0));
		orderItemRepository.save(OrderItem.create(order.getId(), pattern.getId(),
				pattern.getSeller().getId(), pattern.getTitle(), price, PLATFORM_FEE_RATE));

		return new OrderResponse(order.getId(), order.getOrderNo(),
				order.getTotalAmount(), order.getPaymentAmount(), order.getOrderStatus().name());
	}

	/** 데모 결제 완료 → 사용권(구매 도안) 생성. */
	@Transactional
	public DemoPaymentResponse completeDemoPayment(Long userId, Long orderId) {
		CustomerOrder order = orderRepository.findByIdAndBuyerId(orderId, userId)
				.orElseThrow(() -> new ApiException(ErrorCode.ORDER_NOT_FOUND, "주문을 찾을 수 없습니다."));
		List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
		Long firstPattern = items.isEmpty() ? null : items.get(0).getPatternId();

		if (order.getOrderStatus() == OrderStatus.PAID) {
			return new DemoPaymentResponse(order.getOrderStatus().name(), firstPattern); // 멱등
		}
		for (OrderItem item : items) {
			if (libraryRepository.existsByUserIdAndPatternIdAndRevokedAtIsNull(userId, item.getPatternId())) {
				throw new ApiException(ErrorCode.ALREADY_OWNED, "이미 보유한 도안입니다.");
			}
			libraryRepository.save(PatternLibrary.create(userId, item.getPatternId(), item.getId()));
		}
		order.markPaid();
		return new DemoPaymentResponse(order.getOrderStatus().name(), firstPattern);
	}

	@Transactional(readOnly = true)
	public List<OrderListItemResponse> myOrders(Long userId) {
		return orderRepository.findByBuyerIdOrderByOrderedAtDesc(userId).stream()
				.map(o -> new OrderListItemResponse(o.getId(), o.getOrderNo(), o.getTotalAmount(),
						o.getPaymentAmount(), o.getOrderStatus().name(), o.getOrderedAt()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<LibraryItemResponse> myLibrary(Long userId) {
		return libraryRepository.findMyLibrary(userId).stream()
				.map(LibraryItemResponse::from).toList();
	}
}
