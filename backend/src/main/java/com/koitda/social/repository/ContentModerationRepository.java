package com.koitda.social.repository;

import com.koitda.social.domain.ContentModeration;
import com.koitda.social.domain.TargetType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentModerationRepository extends JpaRepository<ContentModeration, Long> {

	Optional<ContentModeration> findByTargetTypeAndTargetId(TargetType targetType, Long targetId);
}
