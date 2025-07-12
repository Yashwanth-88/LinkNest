package com.linknest.linknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.MutedHashtag;
import com.linknest.linknest.entity.User;
import com.linknest.linknest.entity.Hashtag;
import java.util.List;
import java.util.Optional;

public interface MutedHashtagRepository extends JpaRepository<MutedHashtag, Long> {
    List<MutedHashtag> findByUser(User user);
    Optional<MutedHashtag> findByUserAndHashtag(User user, Hashtag hashtag);
    boolean existsByUserAndHashtag(User user, Hashtag hashtag);
    List<MutedHashtag> findByUserId(Long userId);
} 