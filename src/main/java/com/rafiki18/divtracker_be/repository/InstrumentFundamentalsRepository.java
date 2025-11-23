package com.rafiki18.divtracker_be.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rafiki18.divtracker_be.model.InstrumentFundamentals;
import com.rafiki18.divtracker_be.model.InstrumentFundamentals.DataQuality;

@Repository
public interface InstrumentFundamentalsRepository extends JpaRepository<InstrumentFundamentals, String> {

    /**
     * Find fundamentals by ticker (case-insensitive).
     */
    Optional<InstrumentFundamentals> findByTickerIgnoreCase(String ticker);

    /**
     * Find all fundamentals older than specified date.
     */
    List<InstrumentFundamentals> findByLastUpdatedAtBefore(LocalDateTime dateTime);

    /**
     * Find all stale fundamentals (older than 24 hours).
     */
    @Query("SELECT f FROM InstrumentFundamentals f WHERE f.lastUpdatedAt < :cutoffTime")
    List<InstrumentFundamentals> findStaleFundamentals(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find fundamentals by data quality.
     */
    List<InstrumentFundamentals> findByDataQuality(DataQuality dataQuality);

    /**
     * Check if fundamentals exist for ticker.
     */
    boolean existsByTickerIgnoreCase(String ticker);

    /**
     * Find all tickers that need refresh (stale or partial).
     */
    @Query("SELECT f FROM InstrumentFundamentals f WHERE " +
           "f.lastUpdatedAt < :cutoffTime OR " +
           "f.dataQuality = 'PARTIAL' OR " +
           "f.dataQuality = 'STALE'")
    List<InstrumentFundamentals> findFundamentalsNeedingRefresh(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count fundamentals by data quality.
     */
    long countByDataQuality(DataQuality dataQuality);

    /**
     * Find fundamentals updated within time window.
     */
    @Query("SELECT f FROM InstrumentFundamentals f WHERE f.lastUpdatedAt >= :since")
    List<InstrumentFundamentals> findRecentlyUpdated(@Param("since") LocalDateTime since);
}
