package com.linknest.linknest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

import com.linknest.linknest.entity.*;
import com.linknest.linknest.repository.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private GroupMemberRepository groupMemberRepository;
    @Autowired
    private GroupMessageRepository groupMessageRepository;
    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Create a group
    @PostMapping
    public ResponseEntity<?> createGroup(@RequestParam String name) {
        User creator = getCurrentUser();
        Group group = new Group(name, creator);
        group = groupRepository.save(group);
        groupMemberRepository.save(new GroupMember(group, creator));
        return ResponseEntity.ok(group);
    }

    // Add a member
    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMember(@PathVariable Long groupId, @RequestParam Long userId) {
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("Group not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        if (groupMemberRepository.existsByGroupAndUser(group, user)) {
            return ResponseEntity.badRequest().body("User already a member");
        }
        groupMemberRepository.save(new GroupMember(group, user));
        return ResponseEntity.ok("User added");
    }

    // Remove a member
    @DeleteMapping("/{groupId}/members")
    public ResponseEntity<?> removeMember(@PathVariable Long groupId, @RequestParam Long userId) {
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("Group not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        groupMemberRepository.deleteByGroupAndUser(group, user);
        return ResponseEntity.ok("User removed");
    }

    // List group members
    @GetMapping("/{groupId}/members")
    public List<User> getGroupMembers(@PathVariable Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("Group not found"));
        return groupMemberRepository.findByGroup(group).stream().map(GroupMember::getUser).collect(Collectors.toList());
    }

    // List user's groups
    @GetMapping("/my")
    public List<Group> getMyGroups() {
        User user = getCurrentUser();
        return groupMemberRepository.findByUser(user).stream().map(GroupMember::getGroup).collect(Collectors.toList());
    }

    // Send a message
    @PostMapping("/{groupId}/messages")
    public ResponseEntity<?> sendMessage(@PathVariable Long groupId, @RequestParam String content, @RequestParam(required = false) String mediaUrl) {
        User sender = getCurrentUser();
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("Group not found"));
        if (!groupMemberRepository.existsByGroupAndUser(group, sender)) {
            return ResponseEntity.status(403).body("Not a group member");
        }
        GroupMessage message = new GroupMessage(group, sender, content, mediaUrl);
        groupMessageRepository.save(message);
        return ResponseEntity.ok(message);
    }

    // Get group messages
    @GetMapping("/{groupId}/messages")
    public List<GroupMessage> getMessages(@PathVariable Long groupId) {
        Group group = groupRepository.findById(groupId).orElseThrow(() -> new RuntimeException("Group not found"));
        return groupMessageRepository.findByGroupOrderByCreatedAtAsc(group);
    }
} 