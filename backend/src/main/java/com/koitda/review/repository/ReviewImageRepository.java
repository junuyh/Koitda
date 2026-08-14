package com.koitda.review.repository;

import com.koitda.review.domain.ReviewImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

	List<ReviewImage> findByReviewIdOrderBySortOrderAscIdAsc(Long reviewId);

	/** 여러 리뷰의 이미지를 한 번에(목록 화면 N+1 방지). */
	List<ReviewImage> findByReviewIdInOrderBySortOrderAscIdAsc(List<Long> reviewIds);
}
