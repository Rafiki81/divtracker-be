package com.rafiki18.divtracker_be.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rafiki18.divtracker_be.model.InstrumentFundamentals;
import com.rafiki18.divtracker_be.repository.InstrumentFundamentalsRepository;
import com.rafiki18.divtracker_be.service.InstrumentFundamentalsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled job to refresh stale fundamentals data.
 * Runs daily to ensure cached data stays reasonably fresh.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FundamentalsRefreshScheduler {
    
    private final InstrumentFundamentalsRepository repository;
    private final InstrumentFundamentalsService fundamentalsService;
    
    // Rate limiting: max refreshes per batch (Finnhub: 60 calls/minute)
    private static final int MAX_REFRESHES_PER_RUN = 4;
    
    /**
     * Refresh stale fundamentals daily at 4 AM.
     * Prioritizes instruments updated longest ago.
     * Respects API rate limits by batching.
     */
    @Scheduled(cron = "0 0 4 * * *") // Daily at 4 AM
    public void refreshStaleFundamentals() {
        log.info("Starting scheduled fundamentals refresh (Daily at 4 AM)");
        
        try {
            LocalDateTime threshold = LocalDateTime.now().minusHours(24);
            List<InstrumentFundamentals> staleData = repository.findFundamentalsNeedingRefresh(threshold);
            
            if (staleData.isEmpty()) {
                log.info("No stale fundamentals found, skipping refresh");
                return;
            }
            
            log.info("Found {} stale fundamentals, refreshing up to {}", 
                staleData.size(), MAX_REFRESHES_PER_RUN);
            
            int refreshed = 0;
            int failed = 0;
            
            for (InstrumentFundamentals stale : staleData) {
                if (refreshed >= MAX_REFRESHES_PER_RUN) {
                    log.info("Reached max refresh limit ({}), will continue in next run", MAX_REFRESHES_PER_RUN);
                    break;
                }
                
                try {
                    String ticker = stale.getTicker();
                    log.debug("Refreshing fundamentals for {}", ticker);
                    fundamentalsService.refreshFundamentals(ticker);
                    refreshed++;
                    
                    // Rate limiting: sleep 1 second between calls (60 calls/min max)
                    if (refreshed < staleData.size() && refreshed < MAX_REFRESHES_PER_RUN) {
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    log.warn("Failed to refresh fundamentals for {}: {}", 
                        stale.getTicker(), e.getMessage());
                    failed++;
                }
            }
            
            log.info("Scheduled refresh completed: {} refreshed, {} failed, {} remaining", 
                refreshed, failed, Math.max(0, staleData.size() - refreshed - failed));
                
        } catch (Exception e) {
            log.error("Error during scheduled fundamentals refresh", e);
        }
    }
    
    /**
     * Clean up very old fundamentals (> 30 days) that are marked STALE.
     * Runs daily at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * *") // Daily at 3 AM
    public void cleanupOldFundamentals() {
        log.info("Starting cleanup of old fundamentals");
        
        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);
            List<InstrumentFundamentals> veryOld = repository.findByLastUpdatedAtBefore(threshold);
            
            if (veryOld.isEmpty()) {
                log.info("No old fundamentals to clean up");
                return;
            }
            
            log.info("Found {} fundamentals older than 30 days", veryOld.size());
            
            // Only delete if marked STALE (keep COMPLETE data even if old)
            int deleted = 0;
            for (InstrumentFundamentals old : veryOld) {
                if (old.getDataQuality() == InstrumentFundamentals.DataQuality.STALE) {
                    repository.delete(old);
                    deleted++;
                }
            }
            
            log.info("Cleanup completed: {} old STALE records deleted", deleted);
            
        } catch (Exception e) {
            log.error("Error during fundamentals cleanup", e);
        }
    }
}
