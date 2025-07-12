package com.linknest.linknest.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.linknest.linknest.entity.Post;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByUserId(Long userId); // Keep existing method
    Page<Post> findByUserId(Long userId, Pageable pageable); // Keep paginated version
    Page<Post> findByUserIdIn(List<Long> userIds, Pageable pageable);
    Page<Post> findByUserIdInAndMediaUrlIsNotNull(List<Long> userIds, Pageable pageable);
    Page<Post> findByUserIdInOrderByCreatedAtAsc(List<Long> userIds, Pageable pageable);
    Page<Post> findByUserIdInOrderByCreatedAtDesc(List<Long> userIds, Pageable pageable);
    @Query("SELECT p FROM Post p WHERE p.user.id IN :userIds ORDER BY (SELECT COUNT(l) FROM Like l WHERE l.post = p) DESC, p.createdAt DESC")
    Page<Post> findByUserIdInOrderByLikeCountDesc(@Param("userIds") List<Long> userIds, Pageable pageable);
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable); // Add for feed
    @Query("SELECT p FROM Post p WHERE p.user.id IN :userIds AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Post> searchByUserIdInAndKeyword(@Param("userIds") List<Long> userIds, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user.id IN :userIds AND LOWER(p.content) LIKE LOWER(CONCAT('%', :hashtag, '%'))")
    Page<Post> searchByUserIdInAndHashtag(@Param("userIds") List<Long> userIds, @Param("hashtag") String hashtag, Pageable pageable);
    Page<Post> findByUserIdAndPinnedTrueOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Post> findByUserIdOrderByPinnedDescCreatedAtDesc(Long userId, Pageable pageable);
    @Query("""
        SELECT p FROM Post p
        LEFT JOIN Like l ON l.post = p
        WHERE (
            LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        GROUP BY p
        ORDER BY
            CASE WHEN LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) THEN 1 ELSE 2 END,
            COUNT(l) DESC,
            p.createdAt DESC
    """)
    Page<Post> fuzzySearchByKeywordRanked(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN p.hashtags h WHERE LOWER(h.name) = LOWER(:hashtag)")
    Page<Post> findByHashtagNameIgnoreCase(@Param("hashtag") String hashtag, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE LOWER(p.user.username) = LOWER(:username)")
    Page<Post> findByUsernameIgnoreCase(@Param("username") String username, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.likeCount DESC")
    List<Post> findTrendingPosts(@Param("since") java.time.LocalDateTime since, org.springframework.data.domain.Pageable pageable);

    java.util.List<Post> findByUserIdIn(java.util.List<Long> userIds);
}