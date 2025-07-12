package com.linknest.linknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.Like;
import com.linknest.linknest.entity.User;
import com.linknest.linknest.entity.Post;
import com.linknest.linknest.entity.Comment;
import java.util.List;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    List<Like> findByUser(User user);
    List<Like> findByPost(Post post);
    List<Like> findByComment(Comment comment);
    Optional<Like> findByUserAndPost(User user, Post post);
    Optional<Like> findByUserAndComment(User user, Comment comment);
    boolean existsByUserAndPost(User user, Post post);
    boolean existsByUserAndComment(User user, Comment comment);
    long countByPost(Post post);
    long countByComment(Comment comment);
    long countByPostId(Long postId);
    long countByCommentId(Long commentId);
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);
}