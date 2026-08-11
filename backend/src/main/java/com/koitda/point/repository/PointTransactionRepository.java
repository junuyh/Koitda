package com.koitda.point.repository;

import com.koitda.point.domain.PointTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

	/** 리뷰당 1회 적립 판정(중복 지급 방지). */
	boolean existsByReviewIdAndTxType(Long reviewId, String txType);

	/** 회수 시 원 적립 거래를 찾는다. */
	Optional<PointTransaction> findByReviewIdAndTxType(Long reviewId, String txType);

	/** 포인트 내역(POINT-006). 최신순. */
	List<PointTransaction> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
