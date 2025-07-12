package com.linknest.linknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.UserBlock;
import com.linknest.linknest.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    List<UserBlock> findByBlocker(User blocker);
    List<UserBlock> findByBlocked(User blocked);
    Optional<UserBlock> findByBlockerAndBlocked(User blocker, User blocked);
    boolean existsByBlockerAndBlocked(User blocker, User blocked);
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
} 