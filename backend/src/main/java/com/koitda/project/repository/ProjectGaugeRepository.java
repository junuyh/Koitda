package com.koitda.project.repository;

import com.koitda.project.domain.ProjectGauge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectGaugeRepository extends JpaRepository<ProjectGauge, Long> {
	List<ProjectGauge> findByProjectIdOrderBySortOrderAscIdAsc(Long projectId);

	void deleteByProjectId(Long projectId);

	/** 도안에 연결된 니팅로그들의 게이지 분포(코수×단수) 집계. */
	@Query("""
			select g.stitches as stitches, g.rows as rows, count(g) as cnt
			from ProjectGauge g, KnittingProject kp
			where kp.id = g.projectId and kp.sellingPatternId = :patternId and kp.deletedAt is null
			  and g.stitches is not null
			group by g.stitches, g.rows
			order by count(g) desc
			""")
	List<GaugeStatView> topGauges(@Param("patternId") Long patternId);
}
