package com.koitda.pattern.repository;

import com.koitda.pattern.domain.SellingPatternImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SellingPatternImageRepository extends JpaRepository<SellingPatternImage, Long> {

	/** 여러 도안의 대표 이미지를 한 번에 조회(N+1 방지). file 은 저장 키를 위해 fetch join. */
	@Query("select i from SellingPatternImage i join fetch i.file "
			+ "where i.patternId in :patternIds and i.thumbnail = true")
	List<SellingPatternImage> findThumbnails(@Param("patternIds") Collection<Long> patternIds);

	/** 상세 화면용 — 한 도안의 모든 이미지를 정렬 순서대로. */
	@Query("select i from SellingPatternImage i join fetch i.file "
			+ "where i.patternId = :patternId order by i.sortOrder asc, i.id asc")
	List<SellingPatternImage> findByPattern(@Param("patternId") Long patternId);

	/** 초안 이미지 재저장 시 기존 행 제거(전체 교체 방식). */
	void deleteByPatternId(Long patternId);
}
