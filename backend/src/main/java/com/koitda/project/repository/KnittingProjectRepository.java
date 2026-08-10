package com.koitda.project.repository;

import com.koitda.project.domain.KnittingProject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnittingProjectRepository extends JpaRepository<KnittingProject, Long> {

	/** 날짜 기반 제목의 중복 순번 계산용(PROJECT-005). */
	long countByUserIdAndDisplayTitleStartingWith(Long userId, String prefix);

	Optional<KnittingProject> findByIdAndDeletedAtIsNull(Long id);

	/** 코잇기 — 연결 도안 기준으로 니팅로그를 묶는다(PROJECT-002·013). 최근 활동순. */
	@Query(value = """
			SELECT COALESCE(sp.title, ep.title) AS patternTitle,
			       kp.selling_pattern_id          AS sellingPatternId,
			       kp.external_pattern_id          AS externalPatternId,
			       COUNT(*)                        AS projectCount
			FROM knitting_project kp
			LEFT JOIN selling_pattern  sp ON kp.selling_pattern_id  = sp.id
			LEFT JOIN external_pattern ep ON kp.external_pattern_id = ep.id
			WHERE kp.user_id = :userId AND kp.deleted_at IS NULL
			GROUP BY kp.selling_pattern_id, kp.external_pattern_id, sp.title, ep.title
			ORDER BY MAX(kp.created_at) DESC
			""", nativeQuery = true)
	List<PatternGroupView> groupedByPattern(@Param("userId") Long userId);

	/** 내 니팅로그 플랫 목록 — 최신 생성순. 도안명은 판매/외부 도안에서 가져온다. */
	@Query(value = """
			SELECT kp.id                    AS id,
			       kp.display_title          AS displayTitle,
			       kp.status                 AS status,
			       kp.visibility             AS visibility,
			       COALESCE(sp.title, ep.title) AS patternTitle,
			       kp.external_pattern_id     AS externalPatternId,
			       kp.created_at             AS createdAt
			FROM knitting_project kp
			LEFT JOIN selling_pattern  sp ON kp.selling_pattern_id  = sp.id
			LEFT JOIN external_pattern ep ON kp.external_pattern_id = ep.id
			WHERE kp.user_id = :userId AND kp.deleted_at IS NULL
			ORDER BY kp.created_at DESC
			""", nativeQuery = true)
	List<ProjectListView> findMyProjects(@Param("userId") Long userId);
}
