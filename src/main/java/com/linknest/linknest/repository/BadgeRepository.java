package com.linknest.linknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.Badge;

public interface BadgeRepository extends JpaRepository<Badge, Long> {
} 