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
import io.swagger.v3.oas.annotations.media.Schema;
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
     * Convert entity to response DTO - only essential data.
     */
    private FundamentalsResponse toResponse(InstrumentFundamentals fundamentals) {
        return FundamentalsResponse.builder()
                .ticker(fundamentals.getTicker())
                .companyName(fundamentals.getCompanyName())
                .currency(fundamentals.getCurrency())
                .sector(fundamentals.getSector())
                .currentPrice(fundamentals.getCurrentPrice())
                .peAnnual(fundamentals.getPeAnnual())
                .beta(fundamentals.getBeta())
                .debtToEquityRatio(fundamentals.getDebtToEquityRatio())
                .fcfAnnual(fundamentals.getFcfAnnual())
                .fcfPerShareAnnual(fundamentals.getFcfPerShareAnnual())
                .shareOutstanding(fundamentals.getShareOutstanding())
                .dividendPerShareAnnual(fundamentals.getDividendPerShareAnnual())
                .dividendYield(fundamentals.getDividendYield())
                .dividendGrowthRate5Y(fundamentals.getDividendGrowthRate5Y())
                .epsGrowth5Y(fundamentals.getEpsGrowth5Y())
                .revenueGrowth5Y(fundamentals.getRevenueGrowth5Y())
                .dataQuality(fundamentals.getDataQuality().name())
                .dataSource(fundamentals.getSource().name())
                .lastUpdatedAt(fundamentals.getLastUpdatedAt())
                .build();
    }
    
    /**
     * Response DTO for fundamentals endpoint - optimized with essential data only.
     */
    @lombok.Data
    @lombok.Builder
    @Schema(description = "Essential fundamentals data from Finnhub")
    public static class FundamentalsResponse {
        @Schema(description = "Stock ticker symbol", example = "AAPL")
        private String ticker;
        
        @Schema(description = "Company name", example = "Apple Inc")
        private String companyName;
        
        @Schema(description = "Currency", example = "USD")
        private String currency;
        
        @Schema(description = "Business sector", example = "Technology")
        private String sector;
        
        @Schema(description = "Current market price", example = "172.15")
        private java.math.BigDecimal currentPrice;
        
        @Schema(description = "Price-to-Earnings ratio (Annual)", example = "28.50")
        private java.math.BigDecimal peAnnual;
        
        @Schema(description = "Beta (volatility vs market)", example = "1.25")
        private java.math.BigDecimal beta;
        
        @Schema(description = "Debt to Equity ratio", example = "0.6094")
        private java.math.BigDecimal debtToEquityRatio;
        
        @Schema(description = "Free Cash Flow (Annual)", example = "99803000000")
        private java.math.BigDecimal fcfAnnual;
        
        @Schema(description = "Free Cash Flow per share (Annual)", example = "6.50")
        private java.math.BigDecimal fcfPerShareAnnual;
        
        @Schema(description = "Shares outstanding", example = "15550061000")
        private java.math.BigDecimal shareOutstanding;
        
        @Schema(description = "Dividend per share (Annual)", example = "0.96")
        private java.math.BigDecimal dividendPerShareAnnual;
        
        @Schema(description = "Dividend yield percentage", example = "0.52")
        private java.math.BigDecimal dividendYield;
        
        @Schema(description = "Dividend growth rate (5Y)", example = "-2.03")
        private java.math.BigDecimal dividendGrowthRate5Y;

        @Schema(description = "EPS growth rate (5Y)", example = "36.82")
        private java.math.BigDecimal epsGrowth5Y;

        @Schema(description = "Revenue growth rate (5Y)", example = "1.91")
        private java.math.BigDecimal revenueGrowth5Y;
        
        @Schema(description = "Data quality: COMPLETE, PARTIAL, or STALE", example = "COMPLETE")
        private String dataQuality;
        
        @Schema(description = "Data source: FINNHUB, MANUAL, or CALCULATED", example = "FINNHUB")
        private String dataSource;
        
        @Schema(description = "Last time data was updated")
        private java.time.LocalDateTime lastUpdatedAt;
    }
}
