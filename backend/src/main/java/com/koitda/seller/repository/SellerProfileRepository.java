package com.koitda.seller.repository;

import com.koitda.seller.domain.SellerProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {

	boolean existsByUserId(Long userId);

	Optional<SellerProfile> findByUserId(Long userId);
}
