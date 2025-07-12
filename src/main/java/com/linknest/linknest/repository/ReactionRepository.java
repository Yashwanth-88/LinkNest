package com.linknest.linknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.Reaction;
import com.linknest.linknest.entity.User;
import com.linknest.linknest.entity.Post;
import com.linknest.linknest.entity.Comment;
import java.util.List;
import java.util.Optional;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    List<Reaction> findByUser(User user);
    List<Reaction> findByPost(Post post);
    List<Reaction> findByComment(Comment comment);
    Optional<Reaction> findByUserAndPost(User user, Post post);
    Optional<Reaction> findByUserAndComment(User user, Comment comment);
    boolean existsByUserAndPost(User user, Post post);
    boolean existsByUserAndComment(User user, Comment comment);
    List<Reaction> findByPostId(Long postId);
    List<Reaction> findByCommentId(Long commentId);
    List<Reaction> findByUserIdAndPostId(Long userId, Long postId);
    List<Reaction> findByUserIdAndCommentId(Long userId, Long commentId);
    Optional<Reaction> findFirstByUserIdAndPostId(Long userId, Long postId);
    Optional<Reaction> findFirstByUserIdAndCommentId(Long userId, Long commentId);
} 