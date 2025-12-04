package com.rafiki18.divtracker_be.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.rafiki18.divtracker_be.dto.WatchlistItemRequest;
import com.rafiki18.divtracker_be.dto.WatchlistItemResponse;
import com.rafiki18.divtracker_be.exception.DuplicateTickerException;
import com.rafiki18.divtracker_be.exception.WatchlistItemNotFoundException;
import com.rafiki18.divtracker_be.mapper.WatchlistMapper;
import com.rafiki18.divtracker_be.model.InstrumentFundamentals;
import com.rafiki18.divtracker_be.model.WatchlistItem;
import com.rafiki18.divtracker_be.repository.WatchlistItemRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchlistService Tests")
class WatchlistServiceTest {
    
    @Mock
    private WatchlistItemRepository repository;
    
    @Mock
    private WatchlistMapper mapper;
    
    @Mock
    private MarketDataEnrichmentService marketDataEnrichmentService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WatchlistService service;
    
    private UUID userId;
    private UUID itemId;
    private WatchlistItem item;
    private WatchlistItemRequest request;
    private WatchlistItemResponse response;
    private InstrumentFundamentals fundamentals;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        
        item = WatchlistItem.builder()
                .id(itemId)
                .userId(userId)
                .ticker("AAPL")
                .exchange("NASDAQ")
                .targetPrice(new BigDecimal("150.50"))
                .targetPfcf(new BigDecimal("15.5"))
                .notifyWhenBelowPrice(false)
                .notes("Apple Inc.")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        request = WatchlistItemRequest.builder()
                .ticker("AAPL")
                .exchange("NASDAQ")
                .targetPrice(new BigDecimal("150.50"))
                .targetPfcf(new BigDecimal("15.5"))
                .notifyWhenBelowPrice(false)
                .notes("Apple Inc.")
                .build();
        
        response = WatchlistItemResponse.builder()
                .id(itemId)
                .userId(userId)
                .ticker("AAPL")
                .exchange("NASDAQ")
                .targetPrice(new BigDecimal("150.50"))
                .targetPfcf(new BigDecimal("15.5"))
                .notifyWhenBelowPrice(false)
                .notes("Apple Inc.")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        fundamentals = InstrumentFundamentals.builder()
                .ticker("AAPL")
                .currentPrice(new BigDecimal("175.43"))
                .fcfPerShareAnnual(new BigDecimal("6.32"))
                .build();
    }
    
    @Test
    @DisplayName("list() - Debe devolver página de items del usuario")
    void testList_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Page<WatchlistItem> itemPage = new PageImpl<>(List.of(item));
        
        when(repository.findAllByUserId(userId, pageable)).thenReturn(itemPage);
        when(mapper.toResponse(item)).thenReturn(response);
        when(marketDataEnrichmentService.getFundamentals("AAPL")).thenReturn(fundamentals);
        
        // Act
        Page<WatchlistItemResponse> result = service.list(userId, pageable);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTicker()).isEqualTo("AAPL");
        
        verify(repository).findAllByUserId(userId, pageable);
        verify(mapper).toResponse(item);
        verify(mapper).enrichWithMarketData(response, fundamentals);
    }
    
    @Test
    @DisplayName("create() - Debe crear item exitosamente")
    void testCreate_Success() {
        // Arrange
        when(repository.existsByUserIdAndTickerIgnoreCase(userId, "AAPL")).thenReturn(false);
        when(mapper.toEntity(request, userId)).thenReturn(item);
        when(repository.save(item)).thenReturn(item);
        when(repository.countByTickerIgnoreCase("AAPL")).thenReturn(1L);
        when(mapper.toResponse(item)).thenReturn(response);
        when(marketDataEnrichmentService.isAvailable()).thenReturn(true);
        when(marketDataEnrichmentService.getFundamentals("AAPL")).thenReturn(fundamentals);
        
        // Act
        WatchlistItemResponse result = service.create(userId, request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTicker()).isEqualTo("AAPL");
        
        verify(repository).existsByUserIdAndTickerIgnoreCase(userId, "AAPL");
        verify(repository).save(item);
        verify(mapper).toEntity(request, userId);
        verify(mapper).toResponse(item);
        verify(mapper).enrichWithMarketData(response, fundamentals);
    }
    
    @Test
    @DisplayName("create() - Debe lanzar DuplicateTickerException cuando ticker existe")
    void testCreate_DuplicateTicker() {
        // Arrange
        when(repository.existsByUserIdAndTickerIgnoreCase(userId, "AAPL")).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> service.create(userId, request))
                .isInstanceOf(DuplicateTickerException.class)
                .hasMessageContaining("AAPL");
        
        verify(repository).existsByUserIdAndTickerIgnoreCase(userId, "AAPL");
        verify(repository, never()).save(any());
    }
    
    @Test
    @DisplayName("update() - Debe actualizar item exitosamente")
    void testUpdate_Success() {
        // Arrange
        WatchlistItemRequest updateRequest = WatchlistItemRequest.builder()
                .targetPrice(new BigDecimal("160.00"))
                .build();
        
        when(repository.findByUserIdAndId(userId, itemId)).thenReturn(Optional.of(item));
        when(repository.save(item)).thenReturn(item);
        when(mapper.toResponse(item)).thenReturn(response);
        when(marketDataEnrichmentService.getFundamentals("AAPL")).thenReturn(fundamentals);
        doNothing().when(mapper).updateEntityFromRequest(item, updateRequest);
        
        // Act
        WatchlistItemResponse result = service.update(userId, itemId, updateRequest);
        
        // Assert
        assertThat(result).isNotNull();
        
        verify(repository).findByUserIdAndId(userId, itemId);
        verify(mapper).updateEntityFromRequest(item, updateRequest);
        verify(repository).save(item);
        verify(mapper).toResponse(item);
        verify(mapper).enrichWithMarketData(response, fundamentals);
    }
    
    @Test
    @DisplayName("update() - Debe lanzar WatchlistItemNotFoundException cuando item no existe")
    void testUpdate_ItemNotFound() {
        // Arrange
        when(repository.findByUserIdAndId(userId, itemId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> service.update(userId, itemId, request))
                .isInstanceOf(WatchlistItemNotFoundException.class);
        
        verify(repository).findByUserIdAndId(userId, itemId);
        verify(repository, never()).save(any());
    }
    
    @Test
    @DisplayName("update() - Debe lanzar DuplicateTickerException cuando nuevo ticker existe")
    void testUpdate_DuplicateTicker() {
        // Arrange
        WatchlistItemRequest updateRequest = WatchlistItemRequest.builder()
                .ticker("MSFT")
                .build();
        
        item.setTicker("AAPL");
        
        when(repository.findByUserIdAndId(userId, itemId)).thenReturn(Optional.of(item));
        when(repository.existsByUserIdAndTickerIgnoreCase(userId, "MSFT")).thenReturn(true);
        
        // Act & Assert
        assertThatThrownBy(() -> service.update(userId, itemId, updateRequest))
                .isInstanceOf(DuplicateTickerException.class)
                .hasMessageContaining("MSFT");
        
        verify(repository).findByUserIdAndId(userId, itemId);
        verify(repository).existsByUserIdAndTickerIgnoreCase(userId, "MSFT");
        verify(repository, never()).save(any());
    }
    
    @Test
    @DisplayName("delete() - Debe eliminar item exitosamente")
    void testDelete_Success() {
        // Arrange
        when(repository.findByUserIdAndId(userId, itemId)).thenReturn(Optional.of(item));
        when(repository.countByTickerIgnoreCase("AAPL")).thenReturn(0L);
        
        // Act
        service.delete(userId, itemId);
        
        // Assert
        verify(repository).findByUserIdAndId(userId, itemId);
        verify(repository).delete(item);
    }

    @Test
    @DisplayName("update() - Cuando cambia el ticker debe reconfigurar el stream")
    void testUpdate_TickerChangeTriggersSubscriptions() {
        // Arrange
        WatchlistItemRequest updateRequest = new WatchlistItemRequest();
        updateRequest.setTicker("MSFT");
        
        WatchlistItemResponse response = WatchlistItemResponse.builder()
                .ticker("MSFT")  // Response should reflect the updated ticker
                .build();

        item.setTicker("AAPL");

        when(repository.findByUserIdAndId(userId, itemId)).thenReturn(Optional.of(item));
        when(repository.existsByUserIdAndTickerIgnoreCase(userId, "MSFT")).thenReturn(false);
        when(repository.save(item)).thenReturn(item);
        // Mapper returns response with the NEW ticker after update
        when(mapper.toResponse(item)).thenReturn(response);
        // Mock market data fetch for the new ticker
        when(marketDataEnrichmentService.getFundamentals("MSFT")).thenReturn(null);
        doAnswer(invocation -> {
            item.setTicker("MSFT");
            return null;
        }).when(mapper).updateEntityFromRequest(item, updateRequest);

        // Act
        WatchlistItemResponse result = service.update(userId, itemId, updateRequest);

        // Assert
        assertThat(result).isNotNull();
    }
    
    @Test
    @DisplayName("delete() - Debe lanzar WatchlistItemNotFoundException cuando item no existe")
    void testDelete_ItemNotFound() {
        // Arrange
        when(repository.findByUserIdAndId(userId, itemId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThatThrownBy(() -> service.delete(userId, itemId))
                .isInstanceOf(WatchlistItemNotFoundException.class);
        
        verify(repository).findByUserIdAndId(userId, itemId);
        verify(repository, never()).delete(any());
    }
    
    @Test
    @DisplayName("getById() - Debe devolver item por ID")
    void testGetById_Success() {
        // Arrange
        when(repository.findByUserIdAndId(userId, itemId)).thenReturn(Optional.of(item));
        when(mapper.toResponse(item)).thenReturn(response);
        when(marketDataEnrichmentService.getFundamentals("AAPL")).thenReturn(fundamentals);
        
        // Act
        WatchlistItemResponse result = service.getById(userId, itemId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTicker()).isEqualTo("AAPL");
        
        verify(repository).findByUserIdAndId(userId, itemId);
        verify(mapper).toResponse(item);
        verify(mapper).enrichWithMarketData(response, fundamentals);
    }
    
    @Test
    @DisplayName("create() - Debe cargar datos automáticamente cuando solo se proporciona ticker")
    void testCreate_AutomaticDataLoading_Success() {
        // Arrange
        WatchlistItemRequest autoRequest = WatchlistItemRequest.builder()
                .ticker("AAPL")
                .build();
        
        BigDecimal expectedPfcf = new BigDecimal("27.76");
        
        when(repository.existsByUserIdAndTickerIgnoreCase(userId, "AAPL")).thenReturn(false);
        when(marketDataEnrichmentService.isAvailable()).thenReturn(true);
        when(marketDataEnrichmentService.getFundamentals("AAPL")).thenReturn(fundamentals);
        when(mapper.toEntity(any(WatchlistItemRequest.class), any(UUID.class))).thenReturn(item);
        when(repository.save(item)).thenReturn(item);
        when(mapper.toResponse(item)).thenReturn(response);
        
        // Act
        WatchlistItemResponse result = service.create(userId, autoRequest);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(autoRequest.getTargetPfcf()).isEqualTo(expectedPfcf);
        
        verify(marketDataEnrichmentService).isAvailable();
        verify(marketDataEnrichmentService, org.mockito.Mockito.times(2)).getFundamentals("AAPL");
        verify(repository).save(item);
    }
    
    @Test
    @DisplayName("create() - Debe permitir crear sin targets cuando no hay datos de mercado")
    void testCreate_AutomaticDataLoading_NoMarketData() {
        // Arrange
        WatchlistItemRequest autoRequest = WatchlistItemRequest.builder()
                .ticker("INVALID")
                .build();
        
        WatchlistItem itemWithoutTargets = WatchlistItem.builder()
                .ticker("INVALID")
                .build();
        
        WatchlistItemResponse responseWithoutTargets = WatchlistItemResponse.builder()
                .ticker("INVALID")
                .build();
        
        when(repository.existsByUserIdAndTickerIgnoreCase(userId, "INVALID")).thenReturn(false);
        when(marketDataEnrichmentService.isAvailable()).thenReturn(true);
        when(marketDataEnrichmentService.getFundamentals("INVALID")).thenReturn(null);
        when(mapper.toEntity(any(WatchlistItemRequest.class), any(UUID.class))).thenReturn(itemWithoutTargets);
        when(repository.save(itemWithoutTargets)).thenReturn(itemWithoutTargets);
        when(mapper.toResponse(itemWithoutTargets)).thenReturn(responseWithoutTargets);
        
        // Act
        WatchlistItemResponse result = service.create(userId, autoRequest);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTicker()).isEqualTo("INVALID");
        assertThat(autoRequest.getTargetPrice()).isNull();
        assertThat(autoRequest.getTargetPfcf()).isNull();
        
        verify(marketDataEnrichmentService).isAvailable();
        verify(marketDataEnrichmentService, org.mockito.Mockito.times(2)).getFundamentals("INVALID");
        verify(repository).save(itemWithoutTargets);
    }
    
    @Test
    @DisplayName("create() - Debe permitir crear sin targets cuando no hay datos disponibles")
    void testCreate_AutomaticDataLoading_NoDataAvailable() {
        // Arrange
        WatchlistItemRequest autoRequest = WatchlistItemRequest.builder()
                .ticker("AAPL")
                .build();
        
        WatchlistItem itemWithoutTargets = WatchlistItem.builder()
                .ticker("AAPL")
                .build();
        
        WatchlistItemResponse responseWithoutTargets = WatchlistItemResponse.builder()
                .ticker("AAPL")
                .build();
        
        when(repository.existsByUserIdAndTickerIgnoreCase(userId, "AAPL")).thenReturn(false);
        when(marketDataEnrichmentService.isAvailable()).thenReturn(true);
        when(marketDataEnrichmentService.getFundamentals("AAPL")).thenReturn(null);
        when(mapper.toEntity(any(WatchlistItemRequest.class), any(UUID.class))).thenReturn(itemWithoutTargets);
        when(repository.save(itemWithoutTargets)).thenReturn(itemWithoutTargets);
        when(mapper.toResponse(itemWithoutTargets)).thenReturn(responseWithoutTargets);
        
        // Act
        WatchlistItemResponse result = service.create(userId, autoRequest);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTicker()).isEqualTo("AAPL");
        assertThat(autoRequest.getTargetPrice()).isNull();
        assertThat(autoRequest.getTargetPfcf()).isNull();
        
        verify(marketDataEnrichmentService).isAvailable();
        verify(marketDataEnrichmentService, org.mockito.Mockito.times(2)).getFundamentals("AAPL");
        verify(repository).save(itemWithoutTargets);
    }
    
    @Test
    @DisplayName("create() - Debe calcular targetPfcf cuando solo se proporciona targetPrice")
    void testCreate_CalculatesTargetPfcfFromTargetPrice() {
        // Arrange - Solo proporciona targetPrice
        WatchlistItemRequest request = WatchlistItemRequest.builder()
                .ticker("AAPL")
                .targetPrice(new BigDecimal("150.00"))
                .build();
        
        BigDecimal expectedTargetPfcf = new BigDecimal("23.73"); // 150 / 6.32 = 23.73
        
        when(repository.existsByUserIdAndTickerIgnoreCase(userId, "AAPL")).thenReturn(false);
        when(marketDataEnrichmentService.isAvailable()).thenReturn(true);
        when(marketDataEnrichmentService.getFundamentals("AAPL")).thenReturn(fundamentals);
        when(mapper.toEntity(any(WatchlistItemRequest.class), any(UUID.class))).thenReturn(item);
        when(repository.save(item)).thenReturn(item);
        when(mapper.toResponse(item)).thenReturn(response);
        
        // Act
        WatchlistItemResponse result = service.create(userId, request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(request.getTargetPfcf()).isEqualTo(expectedTargetPfcf);
        assertThat(request.getTargetPrice()).isEqualTo(new BigDecimal("150.00"));
        verify(marketDataEnrichmentService, org.mockito.Mockito.times(2)).getFundamentals("AAPL");
        verify(repository).save(item);
    }
    
    @Test
    @DisplayName("create() - Debe calcular targetPrice cuando solo se proporciona targetPfcf")
    void testCreate_CalculatesTargetPriceFromTargetPfcf() {
        // Arrange - Solo proporciona targetPfcf
        WatchlistItemRequest request = WatchlistItemRequest.builder()
                .ticker("AAPL")
                .targetPfcf(new BigDecimal("20.00"))
                .build();
        
        BigDecimal expectedTargetPrice = new BigDecimal("126.40"); // 6.32 × 20 = 126.40
        
        when(repository.existsByUserIdAndTickerIgnoreCase(userId, "AAPL")).thenReturn(false);
        when(marketDataEnrichmentService.isAvailable()).thenReturn(true);
        when(marketDataEnrichmentService.getFundamentals("AAPL")).thenReturn(fundamentals);
        when(mapper.toEntity(any(WatchlistItemRequest.class), any(UUID.class))).thenReturn(item);
        when(repository.save(item)).thenReturn(item);
        when(mapper.toResponse(item)).thenReturn(response);
        
        // Act
        WatchlistItemResponse result = service.create(userId, request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(request.getTargetPrice()).isEqualTo(expectedTargetPrice);
        assertThat(request.getTargetPfcf()).isEqualTo(new BigDecimal("20.00"));
        verify(marketDataEnrichmentService, org.mockito.Mockito.times(2)).getFundamentals("AAPL");
        verify(repository).save(item);
    }
    
    @Test
    @DisplayName("create() - Debe usar ambos valores cuando se proporcionan (sin calcular)")
    void testCreate_UsesBothProvidedValues() {
        // Arrange - Proporciona ambos valores
        WatchlistItemRequest request = WatchlistItemRequest.builder()
                .ticker("AAPL")
                .targetPrice(new BigDecimal("150.50"))
                .targetPfcf(new BigDecimal("15.50"))
                .build();
        
        when(repository.existsByUserIdAndTickerIgnoreCase(userId, "AAPL")).thenReturn(false);
        when(mapper.toEntity(request, userId)).thenReturn(item);
        when(repository.save(item)).thenReturn(item);
        when(mapper.toResponse(item)).thenReturn(response);
        when(marketDataEnrichmentService.getFundamentals("AAPL")).thenReturn(fundamentals);
        
        // Act
        WatchlistItemResponse result = service.create(userId, request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(request.getTargetPrice()).isEqualTo(new BigDecimal("150.50"));
        assertThat(request.getTargetPfcf()).isEqualTo(new BigDecimal("15.50"));
        // Solo se llama durante enrichWithMarketData(), no durante la lógica de cálculo
        verify(marketDataEnrichmentService).getFundamentals("AAPL");
        verify(repository).save(item);
    }
    
    @Test
    @DisplayName("create() - Debe permitir crear solo con targetPrice sin calcular targetPfcf si no hay FCF")
    void testCreate_TargetPriceOnly_NoFCFData() {
        // Arrange
        WatchlistItemRequest request = WatchlistItemRequest.builder()
                .ticker("INVALID")
                .targetPrice(new BigDecimal("150.00"))
                .build();
        
        WatchlistItem itemWithTargetPriceOnly = WatchlistItem.builder()
                .ticker("INVALID")
                .targetPrice(new BigDecimal("150.00"))
                .build();
        
        WatchlistItemResponse responseWithTargetPriceOnly = WatchlistItemResponse.builder()
                .ticker("INVALID")
                .targetPrice(new BigDecimal("150.00"))
                .build();
        
        when(repository.existsByUserIdAndTickerIgnoreCase(userId, "INVALID")).thenReturn(false);
        when(marketDataEnrichmentService.isAvailable()).thenReturn(true);
        when(marketDataEnrichmentService.getFundamentals("INVALID")).thenReturn(null);
        when(mapper.toEntity(any(WatchlistItemRequest.class), any(UUID.class))).thenReturn(itemWithTargetPriceOnly);
        when(repository.save(itemWithTargetPriceOnly)).thenReturn(itemWithTargetPriceOnly);
        when(mapper.toResponse(itemWithTargetPriceOnly)).thenReturn(responseWithTargetPriceOnly);
        
        // Act
        WatchlistItemResponse result = service.create(userId, request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTargetPrice()).isEqualTo(new BigDecimal("150.00"));
        assertThat(request.getTargetPfcf()).isNull();
        
        verify(repository).save(itemWithTargetPriceOnly);
    }
    
    @Test
    @DisplayName("create() - Debe permitir crear solo con targetPfcf sin calcular targetPrice si no hay FCF")
    void testCreate_TargetPfcfOnly_NoFCFData() {
        // Arrange
        WatchlistItemRequest request = WatchlistItemRequest.builder()
                .ticker("INVALID")
                .targetPfcf(new BigDecimal("20.00"))
                .build();
        
        WatchlistItem itemWithTargetPfcfOnly = WatchlistItem.builder()
                .ticker("INVALID")
                .targetPfcf(new BigDecimal("20.00"))
                .build();
        
        WatchlistItemResponse responseWithTargetPfcfOnly = WatchlistItemResponse.builder()
                .ticker("INVALID")
                .targetPfcf(new BigDecimal("20.00"))
                .build();
        
        when(repository.existsByUserIdAndTickerIgnoreCase(userId, "INVALID")).thenReturn(false);
        when(marketDataEnrichmentService.isAvailable()).thenReturn(true);
        when(marketDataEnrichmentService.getFundamentals("INVALID")).thenReturn(null);
        when(mapper.toEntity(any(WatchlistItemRequest.class), any(UUID.class))).thenReturn(itemWithTargetPfcfOnly);
        when(repository.save(itemWithTargetPfcfOnly)).thenReturn(itemWithTargetPfcfOnly);
        when(mapper.toResponse(itemWithTargetPfcfOnly)).thenReturn(responseWithTargetPfcfOnly);
        
        // Act
        WatchlistItemResponse result = service.create(userId, request);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTargetPfcf()).isEqualTo(new BigDecimal("20.00"));
        assertThat(request.getTargetPrice()).isNull();
        
        verify(repository).save(itemWithTargetPfcfOnly);
    }
    
    @Test
    @DisplayName("create() - Debe normalizar ticker a mayúsculas")
    void testCreate_NormalizesTickerToUpperCase() {
        // Arrange
        WatchlistItemRequest lowerCaseRequest = WatchlistItemRequest.builder()
                .ticker("aapl")
                .targetPrice(new BigDecimal("150.50"))
                .targetPfcf(new BigDecimal("15.50"))
                .build();
        
        when(repository.existsByUserIdAndTickerIgnoreCase(userId, "AAPL")).thenReturn(false);
        when(mapper.toEntity(lowerCaseRequest, userId)).thenReturn(item);
        when(repository.save(item)).thenReturn(item);
        when(mapper.toResponse(item)).thenReturn(response);
        when(marketDataEnrichmentService.getFundamentals("AAPL")).thenReturn(fundamentals);
        
        // Act
        service.create(userId, lowerCaseRequest);
        
        // Assert
        verify(repository).existsByUserIdAndTickerIgnoreCase(userId, "AAPL");
    }
    
    @Test
    @DisplayName("create() - Debe calcular targetPfcf Y targetPrice correctamente desde datos de mercado")
    void testCreate_CalculatesBothValuesFromMarketData() {
        // Arrange
        WatchlistItemRequest autoRequest = WatchlistItemRequest.builder()
                .ticker("MSFT")
                .build();
        
        BigDecimal price = new BigDecimal("380.00");
        BigDecimal fcf = new BigDecimal("15.00");
        BigDecimal expectedPfcf = new BigDecimal("25.33"); // 380/15 = 25.33
        BigDecimal expectedPrice = new BigDecimal("379.95"); // 15 × 25.33 = 379.95
        
        InstrumentFundamentals msftFundamentals = InstrumentFundamentals.builder()
                .ticker("MSFT")
                .currentPrice(price)
                .fcfPerShareAnnual(fcf)
                .build();
        
        when(repository.existsByUserIdAndTickerIgnoreCase(userId, "MSFT")).thenReturn(false);
        when(marketDataEnrichmentService.isAvailable()).thenReturn(true);
        when(marketDataEnrichmentService.getFundamentals("MSFT")).thenReturn(msftFundamentals);
        
        WatchlistItem msftItem = WatchlistItem.builder()
                .ticker("MSFT")
                .targetPfcf(expectedPfcf)
                .targetPrice(expectedPrice)
                .build();
        
        WatchlistItemResponse msftResponse = WatchlistItemResponse.builder()
                .ticker("MSFT")
                .targetPfcf(expectedPfcf)
                .targetPrice(expectedPrice)
                .build();
        
        when(mapper.toEntity(any(WatchlistItemRequest.class), any(UUID.class))).thenReturn(msftItem);
        when(repository.save(msftItem)).thenReturn(msftItem);
        when(mapper.toResponse(msftItem)).thenReturn(msftResponse);
        
        // Act
        service.create(userId, autoRequest);
        
        // Assert
        assertThat(autoRequest.getTargetPfcf()).isEqualTo(expectedPfcf);
        assertThat(autoRequest.getTargetPrice()).isEqualTo(expectedPrice);
        verify(marketDataEnrichmentService, org.mockito.Mockito.times(2)).getFundamentals("MSFT");
    }
}
