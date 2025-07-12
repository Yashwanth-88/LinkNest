package com.linknest.linknest.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.Notification;
import com.linknest.linknest.entity.User;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Page<Notification> findByUserId(Long userId, Pageable pageable);
    Page<Notification> findByUserAndIsRead(User user, boolean isRead, Pageable pageable);
    List<Notification> findByIdInAndUser(List<Long> ids, User user);
    Page<Notification> findByUserAndTypeOrderByCreatedAtDesc(User user, String type, Pageable pageable);
}