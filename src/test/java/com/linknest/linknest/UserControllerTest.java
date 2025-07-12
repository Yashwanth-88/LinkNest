package com.linknest.linknest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linknest.linknest.controller.UserController;
import com.linknest.linknest.entity.User;
import com.linknest.linknest.repository.UserRepository;
import com.linknest.linknest.dto.UserProfileRequest;
import com.linknest.linknest.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;
import java.util.Set;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private AuthenticationManager authenticationManager;
    @MockBean
    private JwtUtil jwtUtil;
    private ObjectMapper objectMapper = new ObjectMapper();
    private User user;
    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setBio("Test bio");
        user.setLocation("Test City");
        user.setWebsite("https://test.com");
        user.setProfilePictureUrl("/uploads/profile-pictures/test.jpg");
        user.setInterests(Set.of("java", "spring"));
    }
    @org.springframework.security.test.context.support.WithMockUser(username = "testuser")
    void getUserProfile_success() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.bio").value("Test bio"))
                .andExpect(jsonPath("$.location").value("Test City"))
                .andExpect(jsonPath("$.website").value("https://test.com"));
    }
    @org.springframework.security.test.context.support.WithMockUser(username = "testuser")
    void updateUserProfile_success() throws Exception {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserProfileRequest req = new UserProfileRequest();
        req.setBio("Updated bio");
        req.setLocation("New City");
        req.setWebsite("https://new.com");
        req.setProfilePictureUrl("/uploads/profile-pictures/new.jpg");
        mockMvc.perform(put("/api/users/1/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Updated bio"))
                .andExpect(jsonPath("$.location").value("New City"))
                .andExpect(jsonPath("$.website").value("https://new.com"))
                .andExpect(jsonPath("$.profilePictureUrl").value("/uploads/profile-pictures/new.jpg"));
    }
    @org.springframework.security.test.context.support.WithMockUser(username = "otheruser")
    void updateUserProfile_forbidden() throws Exception {
        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(new User() {{ setId(2L); setUsername("otheruser"); }}));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        UserProfileRequest req = new UserProfileRequest();
        req.setBio("Hacked bio");
        mockMvc.perform(put("/api/users/1/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
    @org.springframework.security.test.context.support.WithMockUser(username = "testuser")
    void getUserProfile_notFound() throws Exception {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound());
    }
} 