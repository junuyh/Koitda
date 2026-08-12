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

	Optional<KnittingProject> findByIdAndUserId(Long id, Long userId);

	/** 휴지통 목록 — 최근 삭제순. */
	List<KnittingProject> findByUserIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(Long userId);

	/** 90일 경과(완전 삭제 대상) 조회. */
	List<KnittingProject> findByPurgeAtBefore(java.time.OffsetDateTime threshold);

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
			       (SELECT pi.file_id FROM project_image pi
			         WHERE pi.project_id = kp.id
			         ORDER BY pi.sort_order ASC LIMIT 1) AS thumbnailFileId,
			       kp.created_at             AS createdAt
			FROM knitting_project kp
			LEFT JOIN selling_pattern  sp ON kp.selling_pattern_id  = sp.id
			LEFT JOIN external_pattern ep ON kp.external_pattern_id = ep.id
			WHERE kp.user_id = :userId AND kp.deleted_at IS NULL
			ORDER BY kp.created_at DESC
			""", nativeQuery = true)
	List<ProjectListView> findMyProjects(@Param("userId") Long userId);

	// 공개 니팅로그 피드(둘러보기). 좋아요수 = 소속 '공개' 오늘의 로그(POST) 좋아요 합산.
	// 대표이미지·좋아요수는 상관 서브쿼리로 파생(별도 컬럼 비정규화 없이 진실의 출처 유지).
	String FEED_SELECT = """
			SELECT kp.id                    AS id,
			       kp.display_title          AS displayTitle,
			       kp.status                 AS status,
			       u.nickname                AS authorNickname,
			       (SELECT pi.file_id FROM project_image pi
			         WHERE pi.project_id = kp.id
			         ORDER BY pi.sort_order ASC LIMIT 1) AS thumbnailFileId,
			       (SELECT COUNT(*) FROM post_like pl
			          JOIN content_post cp ON cp.id = pl.target_id AND pl.target_type = 'POST'
			         WHERE cp.project_id = kp.id AND cp.visibility = 'PUBLIC'
			           AND cp.deleted_at IS NULL) AS likeCount,
			       kp.created_at             AS createdAt
			FROM knitting_project kp
			JOIN users u ON u.id = kp.user_id
			WHERE kp.visibility = 'PUBLIC' AND kp.deleted_at IS NULL
			""";

	/** 공개 피드 — 최신순. */
	@Query(value = FEED_SELECT + " ORDER BY kp.created_at DESC LIMIT :limit OFFSET :offset",
			nativeQuery = true)
	List<ProjectFeedView> findPublicFeedRecent(@Param("limit") int limit, @Param("offset") int offset);

	/** 공개 피드 — 좋아요순(동점은 최신순). */
	@Query(value = FEED_SELECT + " ORDER BY likeCount DESC, kp.created_at DESC LIMIT :limit OFFSET :offset",
			nativeQuery = true)
	List<ProjectFeedView> findPublicFeedByLikes(@Param("limit") int limit, @Param("offset") int offset);
}
