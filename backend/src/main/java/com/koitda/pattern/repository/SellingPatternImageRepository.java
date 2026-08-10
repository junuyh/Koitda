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
}
