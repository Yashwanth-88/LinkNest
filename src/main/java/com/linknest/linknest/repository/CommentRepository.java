package com.linknest.linknest.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.Comment;
import com.linknest.linknest.entity.Post;
import java.util.List;
import com.linknest.linknest.entity.User;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostOrderByCreatedAtDesc(Post post);
    Page<Comment> findByPostOrderByCreatedAtDesc(Post post, Pageable pageable);
    Page<Comment> findByPostId(Long postId, Pageable pageable);
    long countByPostId(Long postId);
    java.util.List<Comment> findByUser(com.linknest.linknest.entity.User user);
}