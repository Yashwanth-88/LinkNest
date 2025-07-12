package com.linknest.linknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.MutedUser;
import com.linknest.linknest.entity.User;
import java.util.List;
import java.util.Optional;

public interface MutedUserRepository extends JpaRepository<MutedUser, Long> {
    List<MutedUser> findByUser(User user);
    Optional<MutedUser> findByUserAndMutedUser(User user, User mutedUser);
    boolean existsByUserAndMutedUser(User user, User mutedUser);
    List<MutedUser> findByUserId(Long userId);
} 