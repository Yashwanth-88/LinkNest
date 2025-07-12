package com.linknest.linknest.repository;

import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import com.linknest.linknest.entity.Like;
import com.linknest.linknest.entity.Post;
import java.util.Optional;

@Repository
public class LikeRepositoryImpl implements LikeRepositoryCustom {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public long countLikesByPostId(Long postId) {
        String sql = "SELECT COUNT(*) FROM LINK_NEST_LIKE WHERE post_id = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, postId);
    }
}