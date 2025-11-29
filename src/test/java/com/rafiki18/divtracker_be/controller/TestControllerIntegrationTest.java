package com.rafiki18.divtracker_be.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
@DisplayName("TestController Integration Tests")
class TestControllerIntegrationTest {

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
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Test")
                .lastName("User")
                .provider(AuthProvider.LOCAL)
                .role(Role.USER)
                .enabled(true)
                .build();
        testUser = userRepository.save(testUser);
        validToken = jwtService.generateToken(testUser);
    }

    @Nested
    @DisplayName("Public Endpoint Tests")
    class PublicEndpointTests {

        @Test
        @DisplayName("Should return 200 for public endpoint without authentication")
        void shouldReturnOkForPublicEndpoint() throws Exception {
            mockMvc.perform(get("/api/test/public"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("This is a public endpoint"));
        }

        @Test
        @DisplayName("Should return 200 for public endpoint with authentication")
        void shouldReturnOkForPublicEndpointWithAuth() throws Exception {
            mockMvc.perform(get("/api/test/public")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("This is a public endpoint"));
        }
    }

    @Nested
    @DisplayName("Protected Endpoint Tests")
    class ProtectedEndpointTests {

        @Test
        @DisplayName("Should return 401 for protected endpoint without authentication")
        void shouldReturnUnauthorizedWithoutAuth() throws Exception {
            mockMvc.perform(get("/api/test/protected"))
                    .andExpect(status().isUnauthorized());
        }


        @Test
        @DisplayName("Should return 401 for protected endpoint with malformed header")
        void shouldReturnUnauthorizedWithMalformedHeader() throws Exception {
            mockMvc.perform(get("/api/test/protected")
                            .header("Authorization", "InvalidFormat " + validToken))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 200 for protected endpoint with valid token")
        void shouldReturnOkWithValidToken() throws Exception {
            mockMvc.perform(get("/api/test/protected")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("This is a protected endpoint"))
                    .andExpect(jsonPath("$.user").value("test@example.com"));
        }

        @Test
        @DisplayName("Should return correct username in response")
        void shouldReturnCorrectUsername() throws Exception {
            // Create another user
            User anotherUser = User.builder()
                    .email("another@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .firstName("Another")
                    .lastName("User")
                    .provider(AuthProvider.LOCAL)
                    .role(Role.USER)
                    .enabled(true)
                    .build();
            anotherUser = userRepository.save(anotherUser);
            String anotherToken = jwtService.generateToken(anotherUser);

            mockMvc.perform(get("/api/test/protected")
                            .header("Authorization", "Bearer " + anotherToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.user").value("another@example.com"));
        }
    }
}
