package com.koitda.project.repository;

import com.koitda.project.domain.ProjectYarn;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectYarnRepository extends JpaRepository<ProjectYarn, Long> {
	List<ProjectYarn> findByProjectIdOrderBySortOrderAscIdAsc(Long projectId);

	/** 도안에 연결된 니팅로그들이 자주 쓴 실 집계(브랜드+실이름). */
	@Query("""
			select y.brand as brand, y.yarnName as yarnName, count(y) as cnt
			from ProjectYarn y, KnittingProject kp
			where kp.id = y.projectId and kp.sellingPatternId = :patternId and kp.deletedAt is null
			  and (y.brand is not null or y.yarnName is not null)
			group by y.brand, y.yarnName
			order by count(y) desc
			""")
	List<YarnStatView> topYarns(@Param("patternId") Long patternId);
}
