package com.koitda.project.repository;

import com.koitda.project.domain.ProjectGauge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectGaugeRepository extends JpaRepository<ProjectGauge, Long> {
	List<ProjectGauge> findByProjectIdOrderBySortOrderAscIdAsc(Long projectId);
}
