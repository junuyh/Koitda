package com.koitda.social.repository;

import com.koitda.social.domain.Follow;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, Follow.Key> {

	List<Follow> findByFollowerId(Long followerId);

	long countByFollowingId(Long followingId);
}
