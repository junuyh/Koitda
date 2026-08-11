package com.koitda.order.repository;

import com.koitda.order.domain.PatternLibrary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatternLibraryRepository extends JpaRepository<PatternLibrary, Long> {

	/** 재구매 차단 판정 — 회수되지 않은 사용권 보유 여부(ORDER-005). */
	boolean existsByUserIdAndPatternIdAndRevokedAtIsNull(Long userId, Long patternId);

	/** 리뷰 작성 자격(REVIEW-002) — 구매 이력. 환불(회수) 후에도 리뷰 근거로 유지되므로 revoked 무관. */
	java.util.Optional<PatternLibrary> findFirstByUserIdAndPatternIdOrderByPurchasedAtAsc(Long userId, Long patternId);

	/** 구매 도안 목록 — 도안명·카테고리·구매일·회수 여부. */
	@Query(value = """
			SELECT pl.pattern_id                 AS patternId,
			       sp.title                       AS patternTitle,
			       pc.name                        AS categoryName,
			       pl.purchased_at                AS purchasedAt,
			       (pl.revoked_at IS NOT NULL)    AS revoked
			FROM pattern_library pl
			JOIN selling_pattern sp ON sp.id = pl.pattern_id
			LEFT JOIN pattern_category pc ON pc.id = sp.category_id
			WHERE pl.user_id = :userId
			ORDER BY pl.purchased_at DESC
			""", nativeQuery = true)
	List<LibraryView> findMyLibrary(@Param("userId") Long userId);
}
