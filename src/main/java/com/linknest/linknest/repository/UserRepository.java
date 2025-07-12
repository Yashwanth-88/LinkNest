package com.linknest.linknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.User;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    java.util.List<User> findTop10ByUsernameStartingWithIgnoreCase(String prefix);
    @Query("SELECT u FROM User u LEFT JOIN u.followers f GROUP BY u.id ORDER BY COUNT(f) DESC")
    List<User> findTrendingUsers(org.springframework.data.domain.Pageable pageable);
}