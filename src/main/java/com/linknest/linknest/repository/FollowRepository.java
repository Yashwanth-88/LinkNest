package com.linknest.linknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.Follow;
import com.linknest.linknest.entity.User;
import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    List<Follow> findByFollower(User follower);
    List<Follow> findByFollowing(User following);
    Optional<Follow> findByFollowerAndFollowing(User follower, User following);
    boolean existsByFollowerAndFollowing(User follower, User following);
} 