package com.koitda.project.repository;

import com.koitda.project.domain.ProjectImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {

	/** 상세용 — 한 니팅로그의 이미지를 정렬 순서대로(파일 fetch join). */
	@Query("select pi from ProjectImage pi join fetch pi.file where pi.projectId = :projectId "
			+ "order by pi.sortOrder asc, pi.id asc")
	List<ProjectImage> findByProject(@Param("projectId") Long projectId);
}
