package com.koitda.order.repository;

import com.koitda.order.domain.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	List<OrderItem> findByOrderId(Long orderId);
}
