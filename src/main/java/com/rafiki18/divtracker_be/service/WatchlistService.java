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
        
        // Obtener datos de mercado (siempre, para calcular valores faltantes)
        BigDecimal currentPrice = null;
        BigDecimal fcfPerShare = null;
        BigDecimal epsGrowth5Y = null;
        
        if (marketDataEnrichmentService.isAvailable()) {
            BigDecimal[] marketData = marketDataEnrichmentService.fetchMarketData(normalizedTicker);
            currentPrice = marketData[0];  // Current price
            fcfPerShare = marketData[1];   // FCF per share
            // marketData[2] = PE TTM
            // marketData[3] = Beta
            epsGrowth5Y = marketData[4];   // EPS Growth 5Y
            // marketData[5] = Revenue Growth 5Y
        }
        
        // Caso 1: No se especificó ningún valor → Intentar auto-calcular con datos de mercado
        if (request.getTargetPrice() == null && request.getTargetPfcf() == null) {
            if (currentPrice != null && fcfPerShare != null && fcfPerShare.compareTo(BigDecimal.ZERO) > 0) {
                // Calcular P/FCF actual como targetPfcf inicial
                BigDecimal actualPfcf = currentPrice.divide(fcfPerShare, 2, java.math.RoundingMode.HALF_UP);
                request.setTargetPfcf(actualPfcf);
                
                // Calcular targetPrice = fcfPerShare × targetPfcf
                BigDecimal calculatedTargetPrice = fcfPerShare.multiply(actualPfcf)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
                request.setTargetPrice(calculatedTargetPrice);
                
                log.info("Auto-calculated targetPfcf={} and targetPrice={} for {} based on current market data", 
                    actualPfcf, calculatedTargetPrice, normalizedTicker);
            } else {
                // Permitir crear sin targets - el usuario puede agregarlos después
                // Agregar nota por defecto para cumplir con constraint
                if (request.getNotes() == null || request.getNotes().trim().isEmpty()) {
                    request.setNotes("Pending market data analysis");
                }
                log.info("Creating watchlist item for {} without target values (market data unavailable)", normalizedTicker);
            }
        }
        // Caso 2: Solo se especificó targetPfcf → Calcular targetPrice si es posible
        else if (request.getTargetPrice() == null && request.getTargetPfcf() != null) {
            if (fcfPerShare != null && fcfPerShare.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal calculatedTargetPrice = fcfPerShare.multiply(request.getTargetPfcf())
                    .setScale(2, java.math.RoundingMode.HALF_UP);
                request.setTargetPrice(calculatedTargetPrice);
                log.info("Calculated targetPrice={} from targetPfcf={} and FCF={} for {}", 
                    calculatedTargetPrice, request.getTargetPfcf(), fcfPerShare, normalizedTicker);
            } else {
                // Permitir solo con targetPfcf (targetPrice se calculará cuando haya datos)
                log.info("Creating watchlist item for {} with targetPfcf={} only (FCF data unavailable for targetPrice calculation)", 
                    normalizedTicker, request.getTargetPfcf());
            }
        }
        // Caso 3: Solo se especificó targetPrice → Calcular targetPfcf si es posible
        else if (request.getTargetPrice() != null && request.getTargetPfcf() == null) {
            if (fcfPerShare != null && fcfPerShare.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal calculatedTargetPfcf = request.getTargetPrice()
                    .divide(fcfPerShare, 2, java.math.RoundingMode.HALF_UP);
                request.setTargetPfcf(calculatedTargetPfcf);
                log.info("Calculated targetPfcf={} from targetPrice={} and FCF={} for {}", 
                    calculatedTargetPfcf, request.getTargetPrice(), fcfPerShare, normalizedTicker);
            } else {
                // Permitir solo con targetPrice (targetPfcf se calculará cuando haya datos)
                log.info("Creating watchlist item for {} with targetPrice={} only (FCF data unavailable for targetPfcf calculation)", 
                    normalizedTicker, request.getTargetPrice());
            }
        }
        // Caso 4: Se especificaron ambos → Usar valores del usuario
        else {
            log.info("Using user-provided targetPrice={} and targetPfcf={} for {}", 
                request.getTargetPrice(), request.getTargetPfcf(), normalizedTicker);
        }

        // Auto-set estimated growth rate if not provided
        if (request.getEstimatedFcfGrowthRate() == null && epsGrowth5Y != null) {
            // Convert percentage (e.g. 36.82) to decimal (0.3682)
            // Cap at reasonable limits (e.g. max 15% to be conservative)
            BigDecimal growthDecimal = epsGrowth5Y.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
            BigDecimal maxGrowth = new BigDecimal("0.15");
            
            if (growthDecimal.compareTo(maxGrowth) > 0) {
                request.setEstimatedFcfGrowthRate(maxGrowth);
            } else if (growthDecimal.compareTo(BigDecimal.ZERO) > 0) {
                request.setEstimatedFcfGrowthRate(growthDecimal);
            } else {
                request.setEstimatedFcfGrowthRate(new BigDecimal("0.05")); // Default 5%
            }
            log.info("Auto-set estimated growth rate to {} for {}", request.getEstimatedFcfGrowthRate(), normalizedTicker);
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
        BigDecimal currentPrice = marketData[0];  // Current price
        BigDecimal fcfPerShare = marketData[1];   // FCF per share
        // marketData[2] = PE TTM (available for future use)
        // marketData[3] = Beta (available for future use)
        
        if (currentPrice != null && fcfPerShare != null) {
            watchlistMapper.enrichWithMarketData(response, currentPrice, fcfPerShare);
        }
    }
}
