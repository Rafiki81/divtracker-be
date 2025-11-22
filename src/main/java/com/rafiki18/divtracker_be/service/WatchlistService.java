package com.rafiki18.divtracker_be.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rafiki18.divtracker_be.dto.WatchlistItemRequest;
import com.rafiki18.divtracker_be.dto.WatchlistItemResponse;
import com.rafiki18.divtracker_be.exception.DuplicateTickerException;
import com.rafiki18.divtracker_be.exception.WatchlistItemNotFoundException;
import com.rafiki18.divtracker_be.mapper.WatchlistMapper;
import com.rafiki18.divtracker_be.marketdata.stream.WatchlistTickerSubscriptionService;
import com.rafiki18.divtracker_be.model.WatchlistItem;
import com.rafiki18.divtracker_be.repository.WatchlistItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchlistService {
    
    private final WatchlistItemRepository watchlistItemRepository;
    private final WatchlistMapper watchlistMapper;
    private final WatchlistTickerSubscriptionService tickerSubscriptionService;
    private final MarketDataEnrichmentService marketDataEnrichmentService;
    
    /**
     * Lista todos los items del watchlist de un usuario con paginación
     */
    @Transactional(readOnly = true)
    public Page<WatchlistItemResponse> list(UUID userId, Pageable pageable) {
        log.debug("Listing watchlist items for user: {}", userId);
        Page<WatchlistItem> items = watchlistItemRepository.findAllByUserId(userId, pageable);
        return items.map(item -> {
            WatchlistItemResponse response = watchlistMapper.toResponse(item);
            enrichWithMarketData(response);
            return response;
        });
    }
    
    /**
     * Crea un nuevo item en el watchlist
     */
    @Transactional
    public WatchlistItemResponse create(UUID userId, WatchlistItemRequest request) {
        log.debug("Creating watchlist item for user: {} with ticker: {}", userId, request.getTicker());
        
        // Validar que no exista el ticker para este usuario
        String normalizedTicker = request.getTicker().trim().toUpperCase();
        if (watchlistItemRepository.existsByUserIdAndTickerIgnoreCase(userId, normalizedTicker)) {
            log.warn("Duplicate ticker {} for user {}", normalizedTicker, userId);
            throw new DuplicateTickerException(normalizedTicker);
        }
        
        // Crear y guardar el item
        WatchlistItem item = watchlistMapper.toEntity(request, userId);
        WatchlistItem savedItem = watchlistItemRepository.save(item);
        tickerSubscriptionService.registerTicker(savedItem.getTicker());
        
        log.info("Created watchlist item {} for user {}", savedItem.getId(), userId);
        WatchlistItemResponse response = watchlistMapper.toResponse(savedItem);
        enrichWithMarketData(response);
        return response;
    }
    
    /**
     * Actualiza un item del watchlist (merge parcial)
     */
    @Transactional
    public WatchlistItemResponse update(UUID userId, UUID id, WatchlistItemRequest request) {
        log.debug("Updating watchlist item {} for user: {}", id, userId);
        
        // Buscar el item y verificar que pertenece al usuario
        WatchlistItem item = watchlistItemRepository.findByUserIdAndId(userId, id)
                .orElseThrow(() -> {
                    log.warn("Watchlist item {} not found for user {}", id, userId);
                    return new WatchlistItemNotFoundException(userId, id);
                });
        String previousTicker = item.getTicker();
        
        // Si se está actualizando el ticker, validar que no exista otro con el mismo ticker
        if (request.getTicker() != null && !request.getTicker().isEmpty()) {
            String normalizedTicker = request.getTicker().trim().toUpperCase();
            if (!item.getTicker().equalsIgnoreCase(normalizedTicker)) {
                if (watchlistItemRepository.existsByUserIdAndTickerIgnoreCase(userId, normalizedTicker)) {
                    log.warn("Duplicate ticker {} for user {}", normalizedTicker, userId);
                    throw new DuplicateTickerException(normalizedTicker);
                }
            }
        }
        
        // Actualizar los campos
        watchlistMapper.updateEntityFromRequest(item, request);
        
        // Guardar
        WatchlistItem updatedItem = watchlistItemRepository.save(item);
        if (request.getTicker() != null && !request.getTicker().isBlank()) {
            String newTicker = updatedItem.getTicker();
            if (!previousTicker.equalsIgnoreCase(newTicker)) {
                tickerSubscriptionService.unregisterTicker(previousTicker);
                tickerSubscriptionService.registerTicker(newTicker);
            }
        }
        
        log.info("Updated watchlist item {} for user {}", id, userId);
        WatchlistItemResponse response = watchlistMapper.toResponse(updatedItem);
        enrichWithMarketData(response);
        return response;
    }
    
    /**
     * Elimina un item del watchlist
     */
    @Transactional
    public void delete(UUID userId, UUID id) {
        log.debug("Deleting watchlist item {} for user: {}", id, userId);
        
        // Buscar el item y verificar que pertenece al usuario
        WatchlistItem item = watchlistItemRepository.findByUserIdAndId(userId, id)
                .orElseThrow(() -> {
                    log.warn("Watchlist item {} not found for user {}", id, userId);
                    return new WatchlistItemNotFoundException(userId, id);
                });
        
        watchlistItemRepository.delete(item);
        tickerSubscriptionService.unregisterTicker(item.getTicker());
        log.info("Deleted watchlist item {} for user {}", id, userId);
    }
    
    /**
     * Obtiene un item específico del watchlist
     */
    @Transactional(readOnly = true)
    public WatchlistItemResponse getById(UUID userId, UUID id) {
        log.debug("Getting watchlist item {} for user: {}", id, userId);
        
        WatchlistItem item = watchlistItemRepository.findByUserIdAndId(userId, id)
                .orElseThrow(() -> {
                    log.warn("Watchlist item {} not found for user {}", id, userId);
                    return new WatchlistItemNotFoundException(userId, id);
                });
        
        WatchlistItemResponse response = watchlistMapper.toResponse(item);
        enrichWithMarketData(response);
        return response;
    }
    
    /**
     * Enriquece la respuesta con datos de mercado y métricas financieras calculadas.
     */
    private void enrichWithMarketData(WatchlistItemResponse response) {
        if (response == null) {
            return;
        }
        
        BigDecimal[] marketData = marketDataEnrichmentService.fetchMarketData(response.getTicker());
        BigDecimal currentPrice = marketData[0];
        BigDecimal fcfPerShare = marketData[1];
        
        if (currentPrice != null && fcfPerShare != null) {
            watchlistMapper.enrichWithMarketData(response, currentPrice, fcfPerShare);
        }
    }
}
