package com.koitda.project.repository;

import com.koitda.project.domain.ProjectNeedle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectNeedleRepository extends JpaRepository<ProjectNeedle, Long> {
	List<ProjectNeedle> findByProjectIdOrderBySortOrderAscIdAsc(Long projectId);

	void deleteByProjectId(Long projectId);

	/** 도안에 연결된 니팅로그들이 자주 쓴 바늘(mm) 집계. */
	@Query("""
			select n.sizeMm as sizeMm, count(n) as cnt
			from ProjectNeedle n, KnittingProject kp
			where kp.id = n.projectId and kp.sellingPatternId = :patternId and kp.deletedAt is null
			  and n.sizeMm is not null
			group by n.sizeMm
			order by count(n) desc
			""")
	List<NeedleStatView> topNeedles(@Param("patternId") Long patternId);
}
