package com.koitda.seller.repository;

import com.koitda.seller.domain.ApplicationStatus;
import com.koitda.seller.domain.SellerApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {

	boolean existsByUserIdAndStatus(Long userId, ApplicationStatus status);
}
