package com.rafiki18.divtracker_be.controller;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rafiki18.divtracker_be.dto.WatchlistItemRequest;
import com.rafiki18.divtracker_be.model.AuthProvider;
import com.rafiki18.divtracker_be.model.Role;
import com.rafiki18.divtracker_be.model.User;
import com.rafiki18.divtracker_be.model.WatchlistItem;
import com.rafiki18.divtracker_be.repository.UserRepository;
import com.rafiki18.divtracker_be.repository.WatchlistItemRepository;
import com.rafiki18.divtracker_be.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("WatchlistController Integration Tests")
class WatchlistControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private WatchlistItemRepository watchlistItemRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtService jwtService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private User testUser;
    private User otherUser;
    private String testUserToken;
    private String otherUserToken;
    private UUID testUserId;
    private UUID otherUserId;
    
    @BeforeEach
    void setUp() {
        watchlistItemRepository.deleteAll();
        userRepository.deleteAll();
        
        // Create test users
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
        testUserId = testUser.getId();
        testUserToken = jwtService.generateToken(testUser);
        
        otherUser = User.builder()
                .email("other@example.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Other")
                .lastName("User")
                .provider(AuthProvider.LOCAL)
                .role(Role.USER)
                .enabled(true)
                .build();
        otherUser = userRepository.save(otherUser);
        otherUserId = otherUser.getId();
        otherUserToken = jwtService.generateToken(otherUser);
    }
    
    @Test
    @DisplayName("POST /api/v1/watchlist - Debe crear item exitosamente")
    void testCreateWatchlistItem_Success() throws Exception {
        // Arrange
        WatchlistItemRequest request = WatchlistItemRequest.builder()
                .ticker("AAPL")
                .exchange("NASDAQ")
                .targetPrice(new BigDecimal("150.50"))
                .targetPfcf(new BigDecimal("15.5"))
                .notifyWhenBelowPrice(false)
                .notes("Apple Inc.")
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/watchlist")
                .header("Authorization", "Bearer " + testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.exchange").value("NASDAQ"))
                .andExpect(jsonPath("$.targetPrice").value(150.50))
                .andExpect(jsonPath("$.targetPfcf").value(15.5))
                .andExpect(jsonPath("$.notes").value("Apple Inc."));
    }
    
    @Test
    @DisplayName("POST /api/v1/watchlist - Debe rechazar sin autenticación")
    void testCreateWatchlistItem_Unauthorized() throws Exception {
        // Arrange
        WatchlistItemRequest request = WatchlistItemRequest.builder()
                .ticker("AAPL")
                .targetPrice(new BigDecimal("150.50"))
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/watchlist")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    @DisplayName("POST /api/v1/watchlist - Debe rechazar ticker duplicado")
    void testCreateWatchlistItem_DuplicateTicker() throws Exception {
        // Arrange
        WatchlistItem existingItem = WatchlistItem.builder()
                .userId(testUserId)
                .ticker("AAPL")
                .targetPrice(new BigDecimal("150.00"))
                .build();
        watchlistItemRepository.save(existingItem);
        
        WatchlistItemRequest request = WatchlistItemRequest.builder()
                .ticker("AAPL")
                .targetPrice(new BigDecimal("160.00"))
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/watchlist")
                .header("Authorization", "Bearer " + testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("AAPL")));
    }
    
    @Test
    @DisplayName("POST /api/v1/watchlist - Debe rechazar datos inválidos")
    void testCreateWatchlistItem_InvalidData() throws Exception {
        // Arrange - sin ticker
        WatchlistItemRequest request = WatchlistItemRequest.builder()
                .targetPrice(new BigDecimal("150.50"))
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/watchlist")
                .header("Authorization", "Bearer " + testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("GET /api/v1/watchlist - Debe listar items del usuario autenticado")
    void testListWatchlistItems_Success() throws Exception {
        // Arrange
        WatchlistItem item1 = WatchlistItem.builder()
                .userId(testUserId)
                .ticker("AAPL")
                .targetPrice(new BigDecimal("150.00"))
                .build();
        WatchlistItem item2 = WatchlistItem.builder()
                .userId(testUserId)
                .ticker("MSFT")
                .targetPrice(new BigDecimal("300.00"))
                .build();
        watchlistItemRepository.save(item1);
        watchlistItemRepository.save(item2);
        
        // Crear item de otro usuario (no debe aparecer)
        WatchlistItem otherUserItem = WatchlistItem.builder()
                .userId(otherUserId)
                .ticker("GOOGL")
                .targetPrice(new BigDecimal("2800.00"))
                .build();
        watchlistItemRepository.save(otherUserItem);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/watchlist")
                .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].ticker", containsInAnyOrder("AAPL", "MSFT")))
                .andExpect(jsonPath("$.content[*].ticker", not(hasItem("GOOGL"))));
    }
    
    @Test
    @DisplayName("GET /api/v1/watchlist/{id} - Debe obtener item por ID")
    void testGetWatchlistItemById_Success() throws Exception {
        // Arrange
        WatchlistItem item = WatchlistItem.builder()
                .userId(testUserId)
                .ticker("AAPL")
                .exchange("NASDAQ")
                .targetPrice(new BigDecimal("150.00"))
                .notes("Test notes")
                .build();
        WatchlistItem savedItem = watchlistItemRepository.save(item);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/watchlist/" + savedItem.getId())
                .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedItem.getId().toString()))
                .andExpect(jsonPath("$.ticker").value("AAPL"))
                .andExpect(jsonPath("$.exchange").value("NASDAQ"));
    }
    
    @Test
    @DisplayName("GET /api/v1/watchlist/{id} - Debe rechazar acceso a item de otro usuario")
    void testGetWatchlistItemById_OtherUserItem() throws Exception {
        // Arrange
        WatchlistItem otherUserItem = WatchlistItem.builder()
                .userId(otherUserId)
                .ticker("GOOGL")
                .targetPrice(new BigDecimal("2800.00"))
                .build();
        WatchlistItem savedItem = watchlistItemRepository.save(otherUserItem);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/watchlist/" + savedItem.getId())
                .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("PATCH /api/v1/watchlist/{id} - Debe actualizar item exitosamente")
    void testUpdateWatchlistItem_Success() throws Exception {
        // Arrange
        WatchlistItem item = WatchlistItem.builder()
                .userId(testUserId)
                .ticker("AAPL")
                .targetPrice(new BigDecimal("150.00"))
                .build();
        WatchlistItem savedItem = watchlistItemRepository.save(item);
        
        WatchlistItemRequest updateRequest = WatchlistItemRequest.builder()
                .targetPrice(new BigDecimal("160.00"))
                .notes("Updated notes")
                .build();
        
        // Act & Assert
        mockMvc.perform(patch("/api/v1/watchlist/" + savedItem.getId())
                .header("Authorization", "Bearer " + testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetPrice").value(160.00))
                .andExpect(jsonPath("$.notes").value("Updated notes"));
    }
    
    @Test
    @DisplayName("PATCH /api/v1/watchlist/{id} - Debe rechazar actualización de item de otro usuario")
    void testUpdateWatchlistItem_OtherUserItem() throws Exception {
        // Arrange
        WatchlistItem otherUserItem = WatchlistItem.builder()
                .userId(otherUserId)
                .ticker("GOOGL")
                .targetPrice(new BigDecimal("2800.00"))
                .build();
        WatchlistItem savedItem = watchlistItemRepository.save(otherUserItem);
        
        WatchlistItemRequest updateRequest = WatchlistItemRequest.builder()
                .targetPrice(new BigDecimal("2900.00"))
                .build();
        
        // Act & Assert
        mockMvc.perform(patch("/api/v1/watchlist/" + savedItem.getId())
                .header("Authorization", "Bearer " + testUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("DELETE /api/v1/watchlist/{id} - Debe eliminar item exitosamente")
    void testDeleteWatchlistItem_Success() throws Exception {
        // Arrange
        WatchlistItem item = WatchlistItem.builder()
                .userId(testUserId)
                .ticker("AAPL")
                .targetPrice(new BigDecimal("150.00"))
                .build();
        WatchlistItem savedItem = watchlistItemRepository.save(item);
        
        // Act & Assert
        mockMvc.perform(delete("/api/v1/watchlist/" + savedItem.getId())
                .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isNoContent());
        
        // Verify deletion
        mockMvc.perform(get("/api/v1/watchlist/" + savedItem.getId())
                .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("DELETE /api/v1/watchlist/{id} - Debe rechazar eliminación de item de otro usuario")
    void testDeleteWatchlistItem_OtherUserItem() throws Exception {
        // Arrange
        WatchlistItem otherUserItem = WatchlistItem.builder()
                .userId(otherUserId)
                .ticker("GOOGL")
                .targetPrice(new BigDecimal("2800.00"))
                .build();
        WatchlistItem savedItem = watchlistItemRepository.save(otherUserItem);
        
        // Act & Assert
        mockMvc.perform(delete("/api/v1/watchlist/" + savedItem.getId())
                .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isNotFound());
        
        // Verify item still exists for other user
        mockMvc.perform(get("/api/v1/watchlist/" + savedItem.getId())
                .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("GET /api/v1/watchlist - Debe soportar paginación y ordenamiento")
    void testListWatchlistItems_Pagination() throws Exception {
        // Arrange
        for (int i = 0; i < 25; i++) {
            WatchlistItem item = WatchlistItem.builder()
                    .userId(testUserId)
                    .ticker("TICK" + i)
                    .targetPrice(new BigDecimal("100.00"))
                    .build();
            watchlistItemRepository.save(item);
        }
        
        // Act & Assert - Primera página
        mockMvc.perform(get("/api/v1/watchlist")
                .header("Authorization", "Bearer " + testUserToken)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3));
        
        // Act & Assert - Segunda página
        mockMvc.perform(get("/api/v1/watchlist")
                .header("Authorization", "Bearer " + testUserToken)
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(10)));
    }
}
