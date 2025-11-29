package com.rafiki18.divtracker_be.controller;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.rafiki18.divtracker_be.model.AuthProvider;
import com.rafiki18.divtracker_be.model.Role;
import com.rafiki18.divtracker_be.model.User;
import com.rafiki18.divtracker_be.repository.UserRepository;
import com.rafiki18.divtracker_be.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AdminController Integration Tests")
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = User.builder()
                .email("admin@example.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Admin")
                .lastName("User")
                .provider(AuthProvider.LOCAL)
                .role(Role.USER)
                .enabled(true)
                .build();
        testUser = userRepository.save(testUser);
        validToken = jwtService.generateToken(testUser);
    }

    @Nested
    @DisplayName("Refresh Fundamentals Endpoint Tests")
    class RefreshFundamentalsTests {

        @Test
        @DisplayName("Should return 401 without authentication")
        void shouldReturnUnauthorizedWithoutAuth() throws Exception {
            mockMvc.perform(post("/api/v1/admin/refresh-fundamentals"))
                    .andExpect(status().isUnauthorized());
        }


        @Test
        @DisplayName("Should start refresh job with valid authentication")
        void shouldStartRefreshJobWithValidAuth() throws Exception {
            mockMvc.perform(post("/api/v1/admin/refresh-fundamentals")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Fundamentals refresh job started")));
        }
    }

    @Nested
    @DisplayName("Cleanup Old Fundamentals Endpoint Tests")
    class CleanupOldFundamentalsTests {

        @Test
        @DisplayName("Should return 401 without authentication")
        void shouldReturnUnauthorizedWithoutAuth() throws Exception {
            mockMvc.perform(post("/api/v1/admin/cleanup-old-fundamentals"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should start cleanup job with valid authentication")
        void shouldStartCleanupJobWithValidAuth() throws Exception {
            mockMvc.perform(post("/api/v1/admin/cleanup-old-fundamentals")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Fundamentals cleanup job started")));
        }
    }

    @Nested
    @DisplayName("Multiple Admin Operations Tests")
    class MultipleOperationsTests {

        @Test
        @DisplayName("Should allow both operations in sequence")
        void shouldAllowBothOperationsInSequence() throws Exception {
            // First call refresh
            mockMvc.perform(post("/api/v1/admin/refresh-fundamentals")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk());

            // Then call cleanup
            mockMvc.perform(post("/api/v1/admin/cleanup-old-fundamentals")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Different users should be able to trigger admin operations")
        void differentUsersShouldTriggerOperations() throws Exception {
            // Create another user
            User anotherUser = User.builder()
                    .email("another-admin@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .firstName("Another")
                    .lastName("Admin")
                    .provider(AuthProvider.LOCAL)
                    .role(Role.USER)
                    .enabled(true)
                    .build();
            anotherUser = userRepository.save(anotherUser);
            String anotherToken = jwtService.generateToken(anotherUser);

            // First user triggers refresh
            mockMvc.perform(post("/api/v1/admin/refresh-fundamentals")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk());

            // Second user triggers cleanup
            mockMvc.perform(post("/api/v1/admin/cleanup-old-fundamentals")
                            .header("Authorization", "Bearer " + anotherToken))
                    .andExpect(status().isOk());
        }
    }
}
