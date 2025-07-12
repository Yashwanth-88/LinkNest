package com.linknest.linknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.Story;
import com.linknest.linknest.entity.User;
import java.util.List;

public interface StoryRepository extends JpaRepository<Story, Long> {
    List<Story> findByUserOrderByCreatedAtDesc(User user);
} 