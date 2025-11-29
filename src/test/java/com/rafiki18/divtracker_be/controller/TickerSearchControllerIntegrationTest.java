package com.rafiki18.divtracker_be.controller;

import java.util.Collections;
import java.util.List;

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

import com.rafiki18.divtracker_be.dto.TickerSearchResult;
import com.rafiki18.divtracker_be.model.AuthProvider;
import com.rafiki18.divtracker_be.model.Role;
import com.rafiki18.divtracker_be.model.User;
import com.rafiki18.divtracker_be.repository.UserRepository;
import com.rafiki18.divtracker_be.security.JwtService;
import com.rafiki18.divtracker_be.service.TickerSearchService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("TickerSearchController Integration Tests")
class TickerSearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private TickerSearchService tickerSearchService;

    private String validToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User testUser = User.builder()
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
        
        // By default, service is available
        when(tickerSearchService.isAvailable()).thenReturn(true);
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Lookup should return 401 without authentication")
        void lookupShouldReturnUnauthorizedWithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/tickers/lookup")
                            .param("symbol", "AAPL"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Search should return 401 without authentication")
        void searchShouldReturnUnauthorizedWithoutAuth() throws Exception {
            mockMvc.perform(get("/api/v1/tickers/search")
                            .param("q", "Apple"))
                    .andExpect(status().isUnauthorized());
        }

    }

    @Nested
    @DisplayName("Lookup Tests")
    class LookupTests {

        @Test
        @DisplayName("Should return results for valid symbol")
        void shouldReturnResultsForValidSymbol() throws Exception {
            List<TickerSearchResult> results = List.of(
                    TickerSearchResult.builder()
                            .symbol("AAPL")
                            .description("Apple Inc")
                            .exchange("NASDAQ")
                            .type("Common Stock")
                            .currency("USD")
                            .build()
            );

            when(tickerSearchService.lookupTicker("AAPL")).thenReturn(results);

            mockMvc.perform(get("/api/v1/tickers/lookup")
                            .param("symbol", "AAPL")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                    .andExpect(jsonPath("$[0].description").value("Apple Inc"))
                    .andExpect(jsonPath("$[0].exchange").value("NASDAQ"))
                    .andExpect(jsonPath("$[0].type").value("Common Stock"));
        }

        @Test
        @DisplayName("Should return empty list for non-existent symbol")
        void shouldReturnEmptyListForNonExistentSymbol() throws Exception {
            when(tickerSearchService.lookupTicker("INVALID")).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/tickers/lookup")
                            .param("symbol", "INVALID")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Should return multiple variations for symbol lookup")
        void shouldReturnMultipleVariations() throws Exception {
            List<TickerSearchResult> results = List.of(
                    TickerSearchResult.builder()
                            .symbol("BAM")
                            .description("Brookfield Asset Management")
                            .exchange("NYSE")
                            .type("Common Stock")
                            .build(),
                    TickerSearchResult.builder()
                            .symbol("BAM.A")
                            .description("Brookfield Asset Management Ltd Class A")
                            .exchange("TSX")
                            .type("Common Stock")
                            .build()
            );

            when(tickerSearchService.lookupTicker("BAM")).thenReturn(results);

            mockMvc.perform(get("/api/v1/tickers/lookup")
                            .param("symbol", "BAM")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].symbol").value("BAM"))
                    .andExpect(jsonPath("$[1].symbol").value("BAM.A"));
        }

        @Test
        @DisplayName("Should return error when symbol parameter is missing")
        void shouldReturnErrorWhenSymbolMissing() throws Exception {
            // Spring throws MissingServletRequestParameterException for required params
            mockMvc.perform(get("/api/v1/tickers/lookup")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().is5xxServerError());  // Missing required param
        }

        @Test
        @DisplayName("Should return 503 when service is not available")
        void shouldReturn503WhenServiceUnavailable() throws Exception {
            when(tickerSearchService.isAvailable()).thenReturn(false);

            mockMvc.perform(get("/api/v1/tickers/lookup")
                            .param("symbol", "AAPL")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isServiceUnavailable());
        }
    }

    @Nested
    @DisplayName("Search Tests")
    class SearchTests {

        @Test
        @DisplayName("Should return multiple results for company name search")
        void shouldReturnMultipleResultsForSearch() throws Exception {
            List<TickerSearchResult> results = List.of(
                    TickerSearchResult.builder()
                            .symbol("AAPL")
                            .description("Apple Inc")
                            .exchange("NASDAQ")
                            .type("Common Stock")
                            .build(),
                    TickerSearchResult.builder()
                            .symbol("APLE")
                            .description("Apple Hospitality REIT Inc")
                            .exchange("NYSE")
                            .type("Common Stock")
                            .build()
            );

            when(tickerSearchService.searchTickers("Apple")).thenReturn(results);

            mockMvc.perform(get("/api/v1/tickers/search")
                            .param("q", "Apple")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                    .andExpect(jsonPath("$[0].description").value("Apple Inc"))
                    .andExpect(jsonPath("$[1].symbol").value("APLE"));
        }

        @Test
        @DisplayName("Should return empty array when no matches found")
        void shouldReturnEmptyArrayWhenNoMatches() throws Exception {
            when(tickerSearchService.searchTickers(anyString())).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/tickers/search")
                            .param("q", "XYZNONEXISTENT")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Should return error when q parameter is missing")
        void shouldReturnErrorWhenQueryMissing() throws Exception {
            // Spring throws MissingServletRequestParameterException for required params
            mockMvc.perform(get("/api/v1/tickers/search")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().is5xxServerError());  // Missing required param
        }

        @Test
        @DisplayName("Should handle partial name matches")
        void shouldHandlePartialNameMatches() throws Exception {
            List<TickerSearchResult> results = List.of(
                    TickerSearchResult.builder()
                            .symbol("MSFT")
                            .description("Microsoft Corporation")
                            .exchange("NASDAQ")
                            .type("Common Stock")
                            .build(),
                    TickerSearchResult.builder()
                            .symbol("MU")
                            .description("Micron Technology Inc")
                            .exchange("NASDAQ")
                            .type("Common Stock")
                            .build()
            );

            when(tickerSearchService.searchTickers("Micro")).thenReturn(results);

            mockMvc.perform(get("/api/v1/tickers/search")
                            .param("q", "Micro")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("Should return 503 when service is not available")
        void shouldReturn503WhenServiceUnavailable() throws Exception {
            when(tickerSearchService.isAvailable()).thenReturn(false);

            mockMvc.perform(get("/api/v1/tickers/search")
                            .param("q", "Apple")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isServiceUnavailable());
        }

        @Test
        @DisplayName("Should search for ETFs")
        void shouldSearchForEtfs() throws Exception {
            List<TickerSearchResult> results = List.of(
                    TickerSearchResult.builder()
                            .symbol("SPY")
                            .description("SPDR S&P 500 ETF Trust")
                            .exchange("NYSE ARCA")
                            .type("ETP")
                            .build(),
                    TickerSearchResult.builder()
                            .symbol("VOO")
                            .description("Vanguard S&P 500 ETF")
                            .exchange("NYSE ARCA")
                            .type("ETP")
                            .build()
            );

            when(tickerSearchService.searchTickers("S&P 500")).thenReturn(results);

            mockMvc.perform(get("/api/v1/tickers/search")
                            .param("q", "S&P 500")
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].type").value("ETP"));
        }
    }

    @Nested
    @DisplayName("Different Users Tests")
    class DifferentUsersTests {

        @Test
        @DisplayName("Different users should be able to search")
        void differentUsersShouldSearch() throws Exception {
            List<TickerSearchResult> results = List.of(
                    TickerSearchResult.builder()
                            .symbol("AAPL")
                            .description("Apple Inc")
                            .exchange("NASDAQ")
                            .type("Common Stock")
                            .build()
            );
            when(tickerSearchService.searchTickers("Apple")).thenReturn(results);

            // First user searches
            mockMvc.perform(get("/api/v1/tickers/search")
                            .param("q", "Apple")
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

            // Second user searches same query
            mockMvc.perform(get("/api/v1/tickers/search")
                            .param("q", "Apple")
                            .header("Authorization", "Bearer " + secondToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].symbol").value("AAPL"));
        }
    }
}
