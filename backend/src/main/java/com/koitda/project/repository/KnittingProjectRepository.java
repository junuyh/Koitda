package com.koitda.project.repository;

import com.koitda.project.domain.KnittingProject;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnittingProjectRepository extends JpaRepository<KnittingProject, Long> {

	/** 날짜 기반 제목의 중복 순번 계산용(PROJECT-005). */
	long countByUserIdAndDisplayTitleStartingWith(Long userId, String prefix);

	Optional<KnittingProject> findByIdAndDeletedAtIsNull(Long id);
}
