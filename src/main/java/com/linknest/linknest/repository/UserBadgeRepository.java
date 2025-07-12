package com.linknest.linknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.UserBadge;
import com.linknest.linknest.entity.User;
import com.linknest.linknest.entity.Badge;
import java.util.List;
import java.util.Optional;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByUser(User user);
    Optional<UserBadge> findByUserAndBadge(User user, Badge badge);
    boolean existsByUserAndBadge(User user, Badge badge);
} 