package com.koitda.social.repository;

import com.koitda.social.domain.Comment;
import com.koitda.social.domain.TargetType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	List<Comment> findByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtAsc(
			TargetType targetType, Long targetId);

	long countByTargetTypeAndTargetIdAndDeletedAtIsNull(TargetType targetType, Long targetId);

	@Query("select c.targetId, count(c) from Comment c "
			+ "where c.targetType = :type and c.targetId in :ids and c.deletedAt is null group by c.targetId")
	List<Object[]> countByTargets(@Param("type") TargetType type, @Param("ids") List<Long> ids);
}
