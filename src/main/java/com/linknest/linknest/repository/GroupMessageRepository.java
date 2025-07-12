package com.linknest.linknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.GroupMessage;
import com.linknest.linknest.entity.Group;
import java.util.List;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {
    List<GroupMessage> findByGroupOrderByCreatedAtAsc(Group group);
} 