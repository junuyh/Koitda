package com.koitda.seller.repository;

import com.koitda.seller.domain.ApplicationStatus;
import com.koitda.seller.domain.SellerApplication;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {

	boolean existsByUserIdAndStatus(Long userId, ApplicationStatus status);

	/** 관리자 판매자 심사 큐. status 로 필터하거나 전체. */
	List<SellerApplication> findByStatusOrderByCreatedAtDesc(ApplicationStatus status);

	List<SellerApplication> findAllByOrderByCreatedAtDesc();
}
