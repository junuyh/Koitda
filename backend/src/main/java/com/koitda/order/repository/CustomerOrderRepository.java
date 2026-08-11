package com.koitda.order.repository;

import com.koitda.order.domain.CustomerOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

	Optional<CustomerOrder> findByIdAndBuyerId(Long id, Long buyerId);

	List<CustomerOrder> findByBuyerIdOrderByOrderedAtDesc(Long buyerId);
}
