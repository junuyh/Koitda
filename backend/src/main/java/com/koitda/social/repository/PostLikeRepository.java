package com.koitda.social.repository;

import com.koitda.social.domain.PostLike;
import com.koitda.social.domain.TargetType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLike.Key> {

	long countByTargetTypeAndTargetId(TargetType targetType, Long targetId);

	/** 목록에서 내가 좋아요한 대상 id 를 한 번에 조회(N+1 방지). */
	@Query("select l.targetId from PostLike l "
			+ "where l.userId = :userId and l.targetType = :type and l.targetId in :ids")
	List<Long> findLikedTargetIds(@Param("userId") Long userId, @Param("type") TargetType type,
			@Param("ids") List<Long> ids);

	/** 대상별 좋아요 수 집계(목록용). */
	@Query("select l.targetId, count(l) from PostLike l "
			+ "where l.targetType = :type and l.targetId in :ids group by l.targetId")
	List<Object[]> countByTargets(@Param("type") TargetType type, @Param("ids") List<Long> ids);
}
