package com.koitda.project.repository;

import com.koitda.project.domain.ProjectNeedle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectNeedleRepository extends JpaRepository<ProjectNeedle, Long> {
	List<ProjectNeedle> findByProjectIdOrderBySortOrderAscIdAsc(Long projectId);
}
