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
