package com.rafiki18.divtracker_be.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.rafiki18.divtracker_be.model.WatchlistItem;

@Repository
public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, UUID> {
    
    /**
     * Encuentra todos los items del watchlist de un usuario con paginación
     */
    Page<WatchlistItem> findAllByUserId(UUID userId, Pageable pageable);
    
    /**
     * Encuentra un item específico por userId e id
     */
    Optional<WatchlistItem> findByUserIdAndId(UUID userId, UUID id);
    
    /**
     * Verifica si existe un ticker para un usuario específico (case insensitive)
     */
    boolean existsByUserIdAndTickerIgnoreCase(UUID userId, String ticker);
    
    /**
     * Encuentra un item por userId y ticker (case insensitive)
     */
    Optional<WatchlistItem> findByUserIdAndTickerIgnoreCase(UUID userId, String ticker);

    /**
     * Obtiene el listado de tickers únicos para todos los usuarios
     */
    @Query("select distinct upper(w.ticker) from WatchlistItem w")
    List<String> findDistinctTickers();
}
