package com.koitda.pattern.repository;

import com.koitda.pattern.domain.SellingPattern;
import com.koitda.pattern.domain.Wish;
import com.koitda.pattern.domain.Wish.WishId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WishRepository extends JpaRepository<Wish, WishId> {

	/**
	 * 멱등 등록. 이미 있으면 아무 일도 하지 않고, 실제 삽입된 행 수(1 또는 0)를 돌려준다.
	 * 복합 PK 충돌을 예외가 아니라 반환값으로 다뤄, 트랜잭션을 오염시키지 않고 카운터 갱신 여부를 판단한다.
	 */
	@Modifying
	@Query(value = "INSERT INTO wish(user_id, pattern_id, created_at) "
			+ "VALUES (:userId, :patternId, now()) ON CONFLICT (user_id, pattern_id) DO NOTHING",
			nativeQuery = true)
	int insertIfAbsent(@Param("userId") Long userId, @Param("patternId") Long patternId);

	/** 해제. 삭제된 행 수(1 또는 0)를 돌려준다. */
	@Modifying
	@Query(value = "DELETE FROM wish WHERE user_id = :userId AND pattern_id = :patternId", nativeQuery = true)
	int deleteWish(@Param("userId") Long userId, @Param("patternId") Long patternId);

	/** 목록 화면에서 각 도안의 위시 여부 표시용 — 페이지에 포함된 도안 중 내가 위시한 것. */
	@Query("select w.id.patternId from Wish w where w.id.userId = :userId and w.id.patternId in :patternIds")
	List<Long> findWishedPatternIds(@Param("userId") Long userId,
			@Param("patternIds") Collection<Long> patternIds);

	/** 내 위시 목록 — 위시한 도안을 최신 등록순으로. (Wish ↔ SellingPattern 엔티티 조인) */
	@Query(value = "select p from Wish w join SellingPattern p on p.id = w.id.patternId "
			+ "join fetch p.seller where w.id.userId = :userId order by w.createdAt desc",
			countQuery = "select count(w) from Wish w where w.id.userId = :userId")
	Page<SellingPattern> findWishedPatterns(@Param("userId") Long userId, Pageable pageable);
}
