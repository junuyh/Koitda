package com.koitda.review.repository;

import com.koitda.review.domain.PatternReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatternReviewRepository extends JpaRepository<PatternReview, Long> {

	/** 도안당 1개(REVIEW-003) 판정 — 활성(미삭제) 리뷰. */
	Optional<PatternReview> findByUserIdAndPatternIdAndDeletedAtIsNull(Long userId, Long patternId);

	boolean existsByUserIdAndPatternIdAndDeletedAtIsNull(Long userId, Long patternId);

	/**
	 * 공개 리뷰 목록(REVIEW-001·006). 공개·정상·미삭제, UFO 제외, FO 우선 → 최신순.
	 * 환불 여부는 조건에 넣지 않는다(LIBRARY-005).
	 */
	@Query("""
			select r from PatternReview r
			where r.patternId = :patternId
			  and r.visibility = 'PUBLIC'
			  and r.moderationStatus = 'NORMAL'
			  and r.deletedAt is null
			  and (r.knittingStatus is null or r.knittingStatus <> 'UFO')
			order by case when r.knittingStatus = 'FO' then 0 else 1 end asc, r.createdAt desc
			""")
	List<PatternReview> findPublicByPattern(@Param("patternId") Long patternId);

	/**
	 * 비정규화 카운터 갱신(SELLING_PATTERN.review_count). flushAutomatically 로 선행 변경(리뷰 논리삭제)을
	 * 먼저 반영한 뒤 벌크 업데이트한다 — 안 그러면 clear 로 미반영 변경이 유실된다.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update com.koitda.pattern.domain.SellingPattern p set p.reviewCount = p.reviewCount + :delta "
			+ "where p.id = :patternId and p.reviewCount + :delta >= 0")
	int addPatternReviewCount(@Param("patternId") Long patternId, @Param("delta") int delta);

	/** 내가 쓴 리뷰 목록(나의 뜨개방). 대상 도안명과 함께 최신순. */
	@Query("""
			select r.id as id, r.patternId as patternId, sp.title as patternTitle,
			       r.title as title, r.contentText as contentText, r.knittingStatus as knittingStatus,
			       r.visibility as visibility, r.likeCount as likeCount, r.createdAt as createdAt
			from PatternReview r, com.koitda.pattern.domain.SellingPattern sp
			where sp.id = r.patternId and r.userId = :userId and r.deletedAt is null
			order by r.createdAt desc
			""")
	List<MyReviewView> findMyReviews(@Param("userId") Long userId);
}
