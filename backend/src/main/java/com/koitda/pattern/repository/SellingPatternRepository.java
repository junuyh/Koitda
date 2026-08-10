package com.koitda.pattern.repository;

import com.koitda.pattern.domain.CraftType;
import com.koitda.pattern.domain.SellingPattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SellingPatternRepository extends JpaRepository<SellingPattern, Long> {

	/**
	 * 카탈로그 목록. APPROVED 도안만 노출한다. 각 필터는 null 이면 조건에서 제외된다.
	 * seller 는 판매자명 표시를 위해 fetch join(단일값 연관이라 페이지네이션과 충돌 없음).
	 * 정렬은 Pageable.sort 로 처리한다.
	 */
	@Query(value = """
			select p from SellingPattern p
				join fetch p.seller
			where p.productStatus = com.koitda.pattern.domain.ProductStatus.APPROVED
				and (:q is null or lower(p.title) like lower(concat('%', :q, '%'))
								or lower(p.designerName) like lower(concat('%', :q, '%')))
				and (:categoryId is null or p.categoryId = :categoryId)
				and (:craftType is null or p.craftType = :craftType)
				and (:difficulty is null or p.difficulty = :difficulty)
				and (:minPrice is null or p.salePrice >= :minPrice)
				and (:maxPrice is null or p.salePrice <= :maxPrice)
			""",
			countQuery = """
			select count(p) from SellingPattern p
			where p.productStatus = com.koitda.pattern.domain.ProductStatus.APPROVED
				and (:q is null or lower(p.title) like lower(concat('%', :q, '%'))
								or lower(p.designerName) like lower(concat('%', :q, '%')))
				and (:categoryId is null or p.categoryId = :categoryId)
				and (:craftType is null or p.craftType = :craftType)
				and (:difficulty is null or p.difficulty = :difficulty)
				and (:minPrice is null or p.salePrice >= :minPrice)
				and (:maxPrice is null or p.salePrice <= :maxPrice)
			""")
	Page<SellingPattern> search(
			@Param("q") String q,
			@Param("categoryId") Long categoryId,
			@Param("craftType") CraftType craftType,
			@Param("difficulty") String difficulty,
			@Param("minPrice") Long minPrice,
			@Param("maxPrice") Long maxPrice,
			Pageable pageable);

	/** 위시 등록·해제 시 비정규화 카운터 갱신(음수 방지). 갱신 후 재조회가 최신값을 보도록 컨텍스트를 비운다. */
	@Modifying(clearAutomatically = true)
	@Query("update SellingPattern p set p.wishCount = p.wishCount + :delta "
			+ "where p.id = :id and p.wishCount + :delta >= 0")
	int addWishCount(@Param("id") Long id, @Param("delta") int delta);
}
