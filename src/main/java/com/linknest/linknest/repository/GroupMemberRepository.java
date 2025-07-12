package com.linknest.linknest.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.linknest.linknest.entity.GroupMember;
import com.linknest.linknest.entity.Group;
import com.linknest.linknest.entity.User;
import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByGroup(Group group);
    List<GroupMember> findByUser(User user);
    boolean existsByGroupAndUser(Group group, User user);
    void deleteByGroupAndUser(Group group, User user);
} 