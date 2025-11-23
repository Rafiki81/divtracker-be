package com.rafiki18.divtracker_be.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rafiki18.divtracker_be.model.InstrumentFundamentals;
import com.rafiki18.divtracker_be.model.User;
import com.rafiki18.divtracker_be.service.InstrumentFundamentalsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for managing instrument fundamentals.
 * Provides endpoints to view and refresh cached fundamental data.
 */
@RestController
@RequestMapping("/api/v1/fundamentals")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Fundamentals", description = "Instrument fundamentals management")
public class FundamentalsController {
    
    private final InstrumentFundamentalsService fundamentalsService;
    
    /**
     * Force refresh fundamentals for a ticker from Finnhub.
     * Requires authentication.
     * 
     * @param ticker Stock ticker symbol
     * @param principal Authenticated user
     * @return Refreshed fundamentals data
     */
    @PostMapping("/{ticker}/refresh")
    @Operation(
        summary = "Refresh fundamentals",
        description = "Force refresh fundamentals data from Finnhub for a specific ticker",
        security = @SecurityRequirement(name = "bearer-auth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "Fundamentals refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Ticker not found or data unavailable"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
        }
    )
    public ResponseEntity<FundamentalsResponse> refreshFundamentals(
            @Parameter(description = "Stock ticker symbol (e.g., AAPL)", required = true)
            @PathVariable String ticker,
            @AuthenticationPrincipal User user) {
        
        log.info("User {} requesting fundamentals refresh for {}", user.getEmail(), ticker);
        
        return fundamentalsService.refreshFundamentals(ticker.toUpperCase())
                .map(fundamentals -> {
                    log.info("Successfully refreshed fundamentals for {}", ticker);
                    return ResponseEntity.ok(toResponse(fundamentals));
                })
                .orElseGet(() -> {
                    log.warn("Could not fetch fundamentals for {}", ticker);
                    return ResponseEntity.notFound().build();
                });
    }
    
    /**
     * Convert entity to response DTO.
     */
    private FundamentalsResponse toResponse(InstrumentFundamentals fundamentals) {
        return FundamentalsResponse.builder()
                .ticker(fundamentals.getTicker())
                .companyName(fundamentals.getCompanyName())
                .currentPrice(fundamentals.getCurrentPrice())
                .fcfPerShareTTM(fundamentals.getFcfPerShareTTM())
                .fcfPerShareAnnual(fundamentals.getFcfPerShareAnnual())
                .peTTM(fundamentals.getPeTTM())
                .beta(fundamentals.getBeta())
                .dividendYield(fundamentals.getDividendYield())
                .epsTTM(fundamentals.getEpsTTM())
                .revenueTTM(fundamentals.getRevenueTTM())
                .sharesOutstanding(fundamentals.getSharesOutstanding())
                .operatingMargin(fundamentals.getOperatingMargin())
                .profitMargin(fundamentals.getProfitMargin())
                .exchange(fundamentals.getExchange())
                .sector(fundamentals.getSector())
                .dataQuality(fundamentals.getDataQuality().name())
                .dataSource(fundamentals.getSource().name())
                .lastUpdatedAt(fundamentals.getLastUpdatedAt())
                .build();
    }
    
    /**
     * Response DTO for fundamentals endpoint.
     */
    @lombok.Data
    @lombok.Builder
    public static class FundamentalsResponse {
        private String ticker;
        private String companyName;
        private java.math.BigDecimal currentPrice;
        private java.math.BigDecimal fcfPerShareTTM;
        private java.math.BigDecimal fcfPerShareAnnual;
        private java.math.BigDecimal peTTM;
        private java.math.BigDecimal beta;
        private java.math.BigDecimal dividendYield;
        private java.math.BigDecimal epsTTM;
        private java.math.BigDecimal revenueTTM;
        private Long sharesOutstanding;
        private java.math.BigDecimal operatingMargin;
        private java.math.BigDecimal profitMargin;
        private String exchange;
        private String sector;
        private String dataQuality;
        private String dataSource;
        private java.time.LocalDateTime lastUpdatedAt;
    }
}
