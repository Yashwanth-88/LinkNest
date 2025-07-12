package com.linknest.linknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.Poll;

public interface PollRepository extends JpaRepository<Poll, Long> {
} 