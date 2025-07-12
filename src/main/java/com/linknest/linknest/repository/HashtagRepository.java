package com.linknest.linknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.Hashtag;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {
    Optional<Hashtag> findByNameIgnoreCase(String name);
    List<Hashtag> findTop10ByNameStartingWithIgnoreCase(String prefix);
    @Query("SELECT h.name, COUNT(p.id) as usageCount FROM Hashtag h JOIN h.posts p WHERE p.createdAt >= :since GROUP BY h.name ORDER BY usageCount DESC")
    List<Object[]> findTrendingHashtags(@Param("since") java.time.LocalDateTime since, org.springframework.data.domain.Pageable pageable);
} 