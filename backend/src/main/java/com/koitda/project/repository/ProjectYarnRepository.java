package com.koitda.project.repository;

import com.koitda.project.domain.ProjectYarn;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectYarnRepository extends JpaRepository<ProjectYarn, Long> {
	List<ProjectYarn> findByProjectIdOrderBySortOrderAscIdAsc(Long projectId);
}
