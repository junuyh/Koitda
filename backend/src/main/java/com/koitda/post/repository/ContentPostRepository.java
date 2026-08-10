package com.koitda.post.repository;

import com.koitda.post.domain.ContentPost;
import com.koitda.post.domain.PostType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
