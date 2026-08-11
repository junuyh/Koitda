package com.koitda.post.repository;

import com.koitda.post.domain.ContentPost;
import com.koitda.post.domain.PostType;
import com.koitda.project.domain.ProjectVisibility;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentPostRepository extends JpaRepository<ContentPost, Long> {

	/** 상태 파생 기준 — 기록일 최신 로그(POST-007). */
	Optional<ContentPost> findFirstByProjectIdAndPostTypeAndDeletedAtIsNullOrderByLogDateDescCreatedAtDesc(
			Long projectId, PostType postType);

	/**
	 * 리뷰 불러오기 후보(REVIEW-005) — 해당 도안에 연결된 본인 니팅로그의 오늘의 로그.
	 * 비공개 로그도 불러올 수 있으므로 visibility 는 조건에 넣지 않는다.
	 */
	@Query(value = """
			SELECT p.id             AS id,
			       p.display_title  AS displayTitle,
			       p.log_date       AS logDate,
			       p.knitting_status AS knittingStatus,
			       p.project_id     AS projectId
			FROM content_post p
			JOIN knitting_project kp ON kp.id = p.project_id
			WHERE p.user_id = :userId
			  AND kp.selling_pattern_id = :patternId
			  AND p.post_type = 'PROJECT_LOG'
			  AND p.deleted_at IS NULL
			ORDER BY p.log_date DESC NULLS LAST, p.created_at DESC
			""", nativeQuery = true)
	List<LoadableLogView> findLoadableLogs(@Param("userId") Long userId, @Param("patternId") Long patternId);

	/** 로그 목록 — 기록일 내림차순(POST-012). */
	List<ContentPost> findByProjectIdAndPostTypeAndDeletedAtIsNullOrderByLogDateDescCreatedAtDesc(
			Long projectId, PostType postType);

	/** 날짜 기반 로그 제목의 중복 순번 계산(POST-004). */
	long countByProjectIdAndPostTypeAndDisplayTitleStartingWith(
			Long projectId, PostType postType, String prefix);

	/** 하향 전파 영향 조회 — 공개 로그 수. */
	long countByProjectIdAndPostTypeAndVisibilityAndDeletedAtIsNull(
			Long projectId, PostType postType, ProjectVisibility visibility);

	/** 하향 전파 — 니팅로그의 공개 로그를 모두 비공개로(PROJECT-019). 영향 행 수 반환. */
	@Modifying(clearAutomatically = true)
	@Query("update ContentPost p set p.visibility = com.koitda.project.domain.ProjectVisibility.PRIVATE "
			+ "where p.projectId = :projectId "
			+ "and p.postType = com.koitda.post.domain.PostType.PROJECT_LOG "
			+ "and p.deletedAt is null "
			+ "and p.visibility = com.koitda.project.domain.ProjectVisibility.PUBLIC")
	int makeProjectLogsPrivate(@Param("projectId") Long projectId);

	/** 니팅로그 휴지통 이동 시 연결 로그를 함께 논리 삭제(PROJECT-016). 삭제된 로그 수 반환. */
	@Modifying(clearAutomatically = true)
	@Query("update ContentPost p set p.deletedAt = :ts, p.purgeAt = :purgeAt "
			+ "where p.projectId = :projectId and p.deletedAt is null")
	int softDeleteByProject(@Param("projectId") Long projectId,
			@Param("ts") java.time.OffsetDateTime ts, @Param("purgeAt") java.time.OffsetDateTime purgeAt);

	/** 복구 시 연결 로그도 함께 복구. */
	@Modifying(clearAutomatically = true)
	@Query("update ContentPost p set p.deletedAt = null, p.purgeAt = null "
			+ "where p.projectId = :projectId and p.deletedAt is not null")
	int restoreByProject(@Param("projectId") Long projectId);
}
