package com.rafiki18.divtracker_be.scheduler;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rafiki18.divtracker_be.model.InstrumentFundamentals;
import com.rafiki18.divtracker_be.repository.InstrumentFundamentalsRepository;
import com.rafiki18.divtracker_be.service.InstrumentFundamentalsService;

@ExtendWith(MockitoExtension.class)
@DisplayName("FundamentalsRefreshScheduler Tests")
class FundamentalsRefreshSchedulerTest {

    @Mock
    private InstrumentFundamentalsRepository repository;

    @Mock
    private InstrumentFundamentalsService fundamentalsService;

    @InjectMocks
    private FundamentalsRefreshScheduler scheduler;

    private InstrumentFundamentals staleFundamentals1;
    private InstrumentFundamentals staleFundamentals2;
    private InstrumentFundamentals veryOldStaleFundamentals;

    @BeforeEach
    void setUp() {
        staleFundamentals1 = InstrumentFundamentals.builder()
                .ticker("AAPL")
                .companyName("Apple Inc.")
                .dataQuality(InstrumentFundamentals.DataQuality.STALE)
                .lastUpdatedAt(LocalDateTime.now().minusHours(30))
                .build();

        staleFundamentals2 = InstrumentFundamentals.builder()
                .ticker("MSFT")
                .companyName("Microsoft Corporation")
                .dataQuality(InstrumentFundamentals.DataQuality.STALE)
                .lastUpdatedAt(LocalDateTime.now().minusHours(48))
                .build();

        veryOldStaleFundamentals = InstrumentFundamentals.builder()
                .ticker("OLD")
                .companyName("Old Corp")
                .dataQuality(InstrumentFundamentals.DataQuality.STALE)
                .lastUpdatedAt(LocalDateTime.now().minusDays(35))
                .build();
    }

    @Test
    @DisplayName("Should skip refresh when no stale fundamentals found")
    void shouldSkipRefreshWhenNoStaleData() {
        // Given
        when(repository.findFundamentalsNeedingRefresh(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        scheduler.refreshStaleFundamentals();

        // Then
        verify(repository).findFundamentalsNeedingRefresh(any(LocalDateTime.class));
        verify(fundamentalsService, never()).refreshFundamentals(anyString());
    }

    @Test
    @DisplayName("Should refresh stale fundamentals")
    void shouldRefreshStaleFundamentals() {
        // Given
        List<InstrumentFundamentals> staleList = List.of(staleFundamentals1, staleFundamentals2);
        when(repository.findFundamentalsNeedingRefresh(any(LocalDateTime.class)))
                .thenReturn(staleList);
        when(fundamentalsService.refreshFundamentals(anyString()))
                .thenReturn(Optional.of(staleFundamentals1));

        // When
        scheduler.refreshStaleFundamentals();

        // Then
        verify(repository).findFundamentalsNeedingRefresh(any(LocalDateTime.class));
        verify(fundamentalsService).refreshFundamentals("AAPL");
        verify(fundamentalsService).refreshFundamentals("MSFT");
    }

    @Test
    @DisplayName("Should respect max refresh limit per batch")
    void shouldRespectMaxRefreshLimit() {
        // Given - create 10 stale fundamentals (more than MAX_REFRESHES_PER_RUN = 4)
        List<InstrumentFundamentals> largeStalelist = new java.util.ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            largeStalelist.add(InstrumentFundamentals.builder()
                    .ticker("TICK" + i)
                    .dataQuality(InstrumentFundamentals.DataQuality.STALE)
                    .lastUpdatedAt(LocalDateTime.now().minusHours(30))
                    .build());
        }
        
        when(repository.findFundamentalsNeedingRefresh(any(LocalDateTime.class)))
                .thenReturn(largeStalelist);
        when(fundamentalsService.refreshFundamentals(anyString()))
                .thenReturn(Optional.of(staleFundamentals1));

        // When
        scheduler.refreshStaleFundamentals();

        // Then - should only refresh 4 (MAX_REFRESHES_PER_RUN)
        verify(fundamentalsService, times(4)).refreshFundamentals(anyString());
    }

    @Test
    @DisplayName("Should continue on individual refresh failure")
    void shouldContinueOnRefreshFailure() {
        // Given
        List<InstrumentFundamentals> staleList = List.of(staleFundamentals1, staleFundamentals2);
        when(repository.findFundamentalsNeedingRefresh(any(LocalDateTime.class)))
                .thenReturn(staleList);
        
        // First refresh fails
        when(fundamentalsService.refreshFundamentals("AAPL"))
                .thenThrow(new RuntimeException("API Error"));
        
        // Second refresh succeeds
        when(fundamentalsService.refreshFundamentals("MSFT"))
                .thenReturn(Optional.of(staleFundamentals2));

        // When
        scheduler.refreshStaleFundamentals();

        // Then - should attempt both despite first failure
        verify(fundamentalsService).refreshFundamentals("AAPL");
        verify(fundamentalsService).refreshFundamentals("MSFT");
    }

    @Test
    @DisplayName("Should not throw exception when scheduler fails")
    void shouldHandleSchedulerException() {
        // Given
        when(repository.findFundamentalsNeedingRefresh(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When/Then - should not throw
        scheduler.refreshStaleFundamentals();
        
        verify(repository).findFundamentalsNeedingRefresh(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should skip cleanup when no old fundamentals found")
    void shouldSkipCleanupWhenNoOldData() {
        // Given
        when(repository.findByLastUpdatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        scheduler.cleanupOldFundamentals();

        // Then
        verify(repository).findByLastUpdatedAtBefore(any(LocalDateTime.class));
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("Should delete old stale fundamentals")
    void shouldDeleteOldStaleFundamentals() {
        // Given
        List<InstrumentFundamentals> oldList = List.of(veryOldStaleFundamentals);
        when(repository.findByLastUpdatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(oldList);

        // When
        scheduler.cleanupOldFundamentals();

        // Then
        verify(repository).findByLastUpdatedAtBefore(any(LocalDateTime.class));
        verify(repository).delete(veryOldStaleFundamentals);
    }

    @Test
    @DisplayName("Should keep old complete fundamentals")
    void shouldKeepOldCompleteFundamentals() {
        // Given
        InstrumentFundamentals oldCompleteFundamentals = InstrumentFundamentals.builder()
                .ticker("KEEP")
                .companyName("Keep Corp")
                .dataQuality(InstrumentFundamentals.DataQuality.COMPLETE)
                .lastUpdatedAt(LocalDateTime.now().minusDays(35))
                .build();
        
        List<InstrumentFundamentals> oldList = List.of(
                veryOldStaleFundamentals,      // should delete (STALE)
                oldCompleteFundamentals         // should keep (COMPLETE)
        );
        
        when(repository.findByLastUpdatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(oldList);

        // When
        scheduler.cleanupOldFundamentals();

        // Then
        verify(repository).delete(veryOldStaleFundamentals);
        verify(repository, never()).delete(oldCompleteFundamentals);
    }

    @Test
    @DisplayName("Should keep old partial fundamentals")
    void shouldKeepOldPartialFundamentals() {
        // Given
        InstrumentFundamentals oldPartialFundamentals = InstrumentFundamentals.builder()
                .ticker("PARTIAL")
                .companyName("Partial Corp")
                .dataQuality(InstrumentFundamentals.DataQuality.PARTIAL)
                .lastUpdatedAt(LocalDateTime.now().minusDays(35))
                .build();
        
        List<InstrumentFundamentals> oldList = List.of(oldPartialFundamentals);
        when(repository.findByLastUpdatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(oldList);

        // When
        scheduler.cleanupOldFundamentals();

        // Then - should keep PARTIAL data
        verify(repository, never()).delete(oldPartialFundamentals);
    }

    @Test
    @DisplayName("Should handle cleanup exception gracefully")
    void shouldHandleCleanupException() {
        // Given
        when(repository.findByLastUpdatedAtBefore(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When/Then - should not throw
        scheduler.cleanupOldFundamentals();
        
        verify(repository).findByLastUpdatedAtBefore(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should handle delete exception and continue cleanup")
    void shouldHandleDeleteException() {
        // Given
        InstrumentFundamentals stale1 = InstrumentFundamentals.builder()
                .ticker("STALE1")
                .dataQuality(InstrumentFundamentals.DataQuality.STALE)
                .lastUpdatedAt(LocalDateTime.now().minusDays(35))
                .build();
        
        InstrumentFundamentals stale2 = InstrumentFundamentals.builder()
                .ticker("STALE2")
                .dataQuality(InstrumentFundamentals.DataQuality.STALE)
                .lastUpdatedAt(LocalDateTime.now().minusDays(35))
                .build();
        
        List<InstrumentFundamentals> oldList = List.of(stale1, stale2);
        when(repository.findByLastUpdatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(oldList);

        // When
        scheduler.cleanupOldFundamentals();

        // Then - should attempt to delete both
        verify(repository).delete(stale1);
        verify(repository).delete(stale2);
    }
}
