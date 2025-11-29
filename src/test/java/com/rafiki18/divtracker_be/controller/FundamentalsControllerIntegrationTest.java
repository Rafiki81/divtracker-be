package com.rafiki18.divtracker_be.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.rafiki18.divtracker_be.model.AuthProvider;
import com.rafiki18.divtracker_be.model.InstrumentFundamentals;
import com.rafiki18.divtracker_be.model.InstrumentFundamentals.DataQuality;
import com.rafiki18.divtracker_be.model.InstrumentFundamentals.DataSource;
import com.rafiki18.divtracker_be.model.Role;
import com.rafiki18.divtracker_be.model.User;
import com.rafiki18.divtracker_be.repository.UserRepository;
import com.rafiki18.divtracker_be.security.JwtService;
import com.rafiki18.divtracker_be.service.InstrumentFundamentalsService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("FundamentalsController Integration Tests")
class FundamentalsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private InstrumentFundamentalsService fundamentalsService;

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
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Should return 401 without authentication")
        void shouldReturnUnauthorizedWithoutAuth() throws Exception {
            mockMvc.perform(post("/api/v1/fundamentals/AAPL/refresh"))
                    .andExpect(status().isUnauthorized());
        }

    }

    @Nested
    @DisplayName("Refresh Fundamentals Tests")
    class RefreshFundamentalsTests {

        @Test
        @DisplayName("Should refresh fundamentals successfully")
        void shouldRefreshFundamentalsSuccessfully() throws Exception {
            InstrumentFundamentals fundamentals = InstrumentFundamentals.builder()
                    .ticker("AAPL")
                    .companyName("Apple Inc")
                    .currency("USD")
                    .sector("Technology")
                    .currentPrice(new BigDecimal("175.50"))
                    .peAnnual(new BigDecimal("28.5"))
                    .fcfPerShareAnnual(new BigDecimal("6.50"))
                    .dividendYield(new BigDecimal("0.52"))
                    .dataQuality(DataQuality.COMPLETE)
                    .source(DataSource.FINNHUB)
                    .lastUpdatedAt(LocalDateTime.now())
                    .build();

            when(fundamentalsService.refreshFundamentals("AAPL")).thenReturn(java.util.Optional.of(fundamentals));

            mockMvc.perform(post("/api/v1/fundamentals/AAPL/refresh")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ticker").value("AAPL"))
                    .andExpect(jsonPath("$.companyName").value("Apple Inc"))
                    .andExpect(jsonPath("$.currentPrice").value(175.50))
                    .andExpect(jsonPath("$.dataQuality").value("COMPLETE"))
                    .andExpect(jsonPath("$.dataSource").value("FINNHUB"));
        }

        @Test
        @DisplayName("Should return 404 when no data available")
        void shouldReturn404WhenNoData() throws Exception {
            when(fundamentalsService.refreshFundamentals(anyString())).thenReturn(java.util.Optional.empty());

            mockMvc.perform(post("/api/v1/fundamentals/INVALID/refresh")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should normalize ticker to uppercase")
        void shouldNormalizeTickerToUppercase() throws Exception {
            InstrumentFundamentals fundamentals = InstrumentFundamentals.builder()
                    .ticker("MSFT")
                    .companyName("Microsoft Corporation")
                    .currency("USD")
                    .currentPrice(new BigDecimal("375.00"))
                    .dataQuality(DataQuality.COMPLETE)
                    .source(DataSource.FINNHUB)
                    .lastUpdatedAt(LocalDateTime.now())
                    .build();

            when(fundamentalsService.refreshFundamentals("MSFT")).thenReturn(java.util.Optional.of(fundamentals));

            // Send lowercase ticker - controller normalizes to uppercase
            mockMvc.perform(post("/api/v1/fundamentals/msft/refresh")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ticker").value("MSFT"));
        }

        @Test
        @DisplayName("Should return all fundamentals fields in response")
        void shouldReturnAllFieldsInResponse() throws Exception {
            InstrumentFundamentals complete = InstrumentFundamentals.builder()
                    .ticker("GOOGL")
                    .companyName("Alphabet Inc")
                    .currency("USD")
                    .sector("Technology")
                    .currentPrice(new BigDecimal("140.25"))
                    .peAnnual(new BigDecimal("25.0"))
                    .beta(new BigDecimal("1.10"))
                    .debtToEquityRatio(new BigDecimal("0.25"))
                    .fcfAnnual(new BigDecimal("60000000000"))
                    .fcfPerShareAnnual(new BigDecimal("5.00"))
                    .shareOutstanding(new BigDecimal("12500000000"))
                    .dividendPerShareAnnual(new BigDecimal("0.80"))
                    .dividendYield(new BigDecimal("0.57"))
                    .dividendGrowthRate5Y(new BigDecimal("10.5"))
                    .epsGrowth5Y(new BigDecimal("15.2"))
                    .revenueGrowth5Y(new BigDecimal("12.8"))
                    .focfCagr5Y(new BigDecimal("14.0"))
                    .dataQuality(DataQuality.COMPLETE)
                    .source(DataSource.FINNHUB)
                    .lastUpdatedAt(LocalDateTime.now())
                    .build();

            when(fundamentalsService.refreshFundamentals("GOOGL")).thenReturn(java.util.Optional.of(complete));

            mockMvc.perform(post("/api/v1/fundamentals/GOOGL/refresh")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ticker").value("GOOGL"))
                    .andExpect(jsonPath("$.companyName").value("Alphabet Inc"))
                    .andExpect(jsonPath("$.currency").value("USD"))
                    .andExpect(jsonPath("$.sector").value("Technology"))
                    .andExpect(jsonPath("$.currentPrice").value(140.25))
                    .andExpect(jsonPath("$.peAnnual").value(25.0))
                    .andExpect(jsonPath("$.beta").value(1.10))
                    .andExpect(jsonPath("$.dividendYield").value(0.57))
                    .andExpect(jsonPath("$.dividendGrowthRate5Y").value(10.5))
                    .andExpect(jsonPath("$.epsGrowth5Y").value(15.2))
                    .andExpect(jsonPath("$.dataQuality").value("COMPLETE"))
                    .andExpect(jsonPath("$.dataSource").value("FINNHUB"))
                    .andExpect(jsonPath("$.lastUpdatedAt").exists());
        }

        @Test
        @DisplayName("Should handle partial data quality")
        void shouldHandlePartialDataQuality() throws Exception {
            InstrumentFundamentals partial = InstrumentFundamentals.builder()
                    .ticker("XYZ")
                    .companyName("XYZ Corp")
                    .currency("USD")
                    .currentPrice(new BigDecimal("50.00"))
                    .dataQuality(DataQuality.PARTIAL)
                    .source(DataSource.FINNHUB)
                    .lastUpdatedAt(LocalDateTime.now())
                    .build();

            when(fundamentalsService.refreshFundamentals("XYZ")).thenReturn(java.util.Optional.of(partial));

            mockMvc.perform(post("/api/v1/fundamentals/XYZ/refresh")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ticker").value("XYZ"))
                    .andExpect(jsonPath("$.dataQuality").value("PARTIAL"));
        }
    }

    @Nested
    @DisplayName("Different Users Tests")
    class DifferentUsersTests {

        @Test
        @DisplayName("Different users should be able to refresh same ticker")
        void differentUsersShouldRefreshSameTicker() throws Exception {
            InstrumentFundamentals fundamentals = InstrumentFundamentals.builder()
                    .ticker("AAPL")
                    .companyName("Apple Inc")
                    .currency("USD")
                    .currentPrice(new BigDecimal("175.00"))
                    .dataQuality(DataQuality.COMPLETE)
                    .source(DataSource.FINNHUB)
                    .lastUpdatedAt(LocalDateTime.now())
                    .build();

            when(fundamentalsService.refreshFundamentals("AAPL")).thenReturn(java.util.Optional.of(fundamentals));

            // First user refreshes
            mockMvc.perform(post("/api/v1/fundamentals/AAPL/refresh")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk());

            // Create second user
            User secondUser = User.builder()
                    .email("second@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .firstName("Second")
                    .lastName("User")
                    .provider(AuthProvider.LOCAL)
                    .role(Role.USER)
                    .enabled(true)
                    .build();
            secondUser = userRepository.save(secondUser);
            String secondToken = jwtService.generateToken(secondUser);

            // Second user refreshes same ticker
            mockMvc.perform(post("/api/v1/fundamentals/AAPL/refresh")
                            .header("Authorization", "Bearer " + secondToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ticker").value("AAPL"));
        }
    }
}
