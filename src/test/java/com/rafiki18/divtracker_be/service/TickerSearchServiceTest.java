package com.rafiki18.divtracker_be.service;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rafiki18.divtracker_be.dto.TickerSearchResult;
import com.rafiki18.divtracker_be.marketdata.FinnhubClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("TickerSearchService Tests")
class TickerSearchServiceTest {
    
    @Mock
    private FinnhubClient finnhubClient;
    
    @InjectMocks
    private TickerSearchService service;
    
    private List<TickerSearchResult> mockResults;
    
    @BeforeEach
    void setUp() {
        mockResults = List.of(
            TickerSearchResult.builder()
                .symbol("AAPL")
                .description("Apple Inc")
                .type("Common Stock")
                .exchange("NASDAQ")
                .currency("USD")
                .figi("BBG000B9XRY4")
                .build(),
            TickerSearchResult.builder()
                .symbol("AAPL.SW")
                .description("Apple Inc")
                .type("Common Stock")
                .exchange("SIX")
                .currency("CHF")
                .figi("BBG000B9Y5X2")
                .build()
        );
    }
    
    @Test
    @DisplayName("searchTickers() - Debe usar searchSymbols directamente")
    void testSearchTickers_Success() {
        // Arrange
        String query = "AAPL";
        when(finnhubClient.isEnabled()).thenReturn(true);
        when(finnhubClient.searchSymbols(query)).thenReturn(mockResults);
        
        // Act
        List<TickerSearchResult> results = service.searchTickers(query);
        
        // Assert
        assertThat(results).isNotNull();
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getSymbol()).isEqualTo("AAPL");
        assertThat(results.get(0).getDescription()).isEqualTo("Apple Inc");
        assertThat(results.get(1).getSymbol()).isEqualTo("AAPL.SW");
        
        verify(finnhubClient).isEnabled();
        verify(finnhubClient).searchSymbols(query);
        verify(finnhubClient, org.mockito.Mockito.never()).lookupSymbol(query);
    }
    
    @Test
    @DisplayName("searchTickers() - Debe retornar lista vacía cuando query es null")
    void testSearchTickers_NullQuery() {
        // Act
        List<TickerSearchResult> results = service.searchTickers(null);
        
        // Assert
        assertThat(results).isEmpty();
        verifyNoInteractions(finnhubClient);
    }
    
    @Test
    @DisplayName("searchTickers() - Debe retornar lista vacía cuando query está vacío")
    void testSearchTickers_EmptyQuery() {
        // Act
        List<TickerSearchResult> results = service.searchTickers("   ");
        
        // Assert
        assertThat(results).isEmpty();
        verifyNoInteractions(finnhubClient);
    }
    
    @Test
    @DisplayName("searchTickers() - Debe retornar lista vacía cuando Finnhub está deshabilitado")
    void testSearchTickers_FinnhubDisabled() {
        // Arrange
        String query = "apple";
        when(finnhubClient.isEnabled()).thenReturn(false);
        
        // Act
        List<TickerSearchResult> results = service.searchTickers(query);
        
        // Assert
        assertThat(results).isEmpty();
        verify(finnhubClient).isEnabled();
        verify(finnhubClient, org.mockito.Mockito.never()).lookupSymbol(query);
        verify(finnhubClient, org.mockito.Mockito.never()).searchSymbols(query);
    }
    
    @Test
    @DisplayName("searchTickers() - Debe normalizar query eliminando espacios")
    void testSearchTickers_TrimsQuery() {
        // Arrange
        String query = "  AAPL  ";
        String trimmedQuery = "AAPL";
        when(finnhubClient.isEnabled()).thenReturn(true);
        when(finnhubClient.searchSymbols(trimmedQuery)).thenReturn(mockResults);
        
        // Act
        List<TickerSearchResult> results = service.searchTickers(query);
        
        // Assert
        assertThat(results).hasSize(2);
        verify(finnhubClient).searchSymbols(trimmedQuery);
    }
    
    @Test
    @DisplayName("searchTickers() - Debe manejar respuesta vacía de Finnhub")
    void testSearchTickers_EmptyResults() {
        // Arrange
        String query = "NONEXISTENT";
        when(finnhubClient.isEnabled()).thenReturn(true);
        when(finnhubClient.searchSymbols(query)).thenReturn(Collections.emptyList());
        
        // Act
        List<TickerSearchResult> results = service.searchTickers(query);
        
        // Assert
        assertThat(results).isEmpty();
        verify(finnhubClient).searchSymbols(query);
        verify(finnhubClient, org.mockito.Mockito.never()).lookupSymbol(query);
    }
    
    @Test
    @DisplayName("searchTickers() - Debe buscar por símbolo usando searchSymbols")
    void testSearchTickers_BySymbol() {
        // Arrange
        String query = "AAPL";
        when(finnhubClient.isEnabled()).thenReturn(true);
        when(finnhubClient.searchSymbols(query)).thenReturn(mockResults);
        
        // Act
        List<TickerSearchResult> results = service.searchTickers(query);
        
        // Assert
        assertThat(results).hasSize(2);
        verify(finnhubClient).searchSymbols(query);
        verify(finnhubClient, org.mockito.Mockito.never()).lookupSymbol(query);
    }
    
    @Test
    @DisplayName("searchTickers() - Debe buscar por nombre usando searchSymbols")
    void testSearchTickers_PartialName() {
        // Arrange
        String query = "molson coors";
        when(finnhubClient.isEnabled()).thenReturn(true);
        when(finnhubClient.searchSymbols(query)).thenReturn(mockResults);
        
        // Act
        List<TickerSearchResult> results = service.searchTickers(query);
        
        // Assert
        assertThat(results).hasSize(2);
        verify(finnhubClient).searchSymbols(query);
        verify(finnhubClient, org.mockito.Mockito.never()).lookupSymbol(query);
    }
    
    @Test
    @DisplayName("isAvailable() - Debe retornar true cuando Finnhub está habilitado")
    void testIsAvailable_Enabled() {
        // Arrange
        when(finnhubClient.isEnabled()).thenReturn(true);
        
        // Act
        boolean available = service.isAvailable();
        
        // Assert
        assertThat(available).isTrue();
        verify(finnhubClient).isEnabled();
    }
    
    @Test
    @DisplayName("isAvailable() - Debe retornar false cuando Finnhub está deshabilitado")
    void testIsAvailable_Disabled() {
        // Arrange
        when(finnhubClient.isEnabled()).thenReturn(false);
        
        // Act
        boolean available = service.isAvailable();
        
        // Assert
        assertThat(available).isFalse();
        verify(finnhubClient).isEnabled();
    }
    
    @Test
    @DisplayName("searchTickers() - Debe manejar diferentes tipos de instrumentos")
    void testSearchTickers_DifferentInstrumentTypes() {
        // Arrange
        List<TickerSearchResult> diverseResults = List.of(
            TickerSearchResult.builder()
                .symbol("AAPL")
                .description("Apple Inc")
                .type("Common Stock")
                .build(),
            TickerSearchResult.builder()
                .symbol("AAPL230616C00150000")
                .description("Apple Inc Call Option")
                .type("Option")
                .build()
        );
        
        when(finnhubClient.isEnabled()).thenReturn(true);
        when(finnhubClient.searchSymbols("apple")).thenReturn(diverseResults);
        
        // Act
        List<TickerSearchResult> results = service.searchTickers("apple");
        
        // Assert
        assertThat(results).hasSize(2);
        assertThat(results).extracting(TickerSearchResult::getType)
                .containsExactly("Common Stock", "Option");
        verify(finnhubClient).searchSymbols("apple");
        verify(finnhubClient, org.mockito.Mockito.never()).lookupSymbol("apple");
    }
    
    @Test
    @DisplayName("searchTickers() - Debe manejar resultados con información completa")
    void testSearchTickers_CompleteInformation() {
        // Arrange
        TickerSearchResult completeResult = TickerSearchResult.builder()
                .symbol("MSFT")
                .description("Microsoft Corporation")
                .type("Common Stock")
                .exchange("NASDAQ")
                .currency("USD")
                .figi("BBG000BPH459")
                .build();
        
        when(finnhubClient.isEnabled()).thenReturn(true);
        when(finnhubClient.searchSymbols("microsoft")).thenReturn(List.of(completeResult));
        
        // Act
        List<TickerSearchResult> results = service.searchTickers("microsoft");
        
        // Assert
        assertThat(results).hasSize(1);
        TickerSearchResult result = results.get(0);
        assertThat(result.getSymbol()).isEqualTo("MSFT");
        assertThat(result.getDescription()).isEqualTo("Microsoft Corporation");
        assertThat(result.getType()).isEqualTo("Common Stock");
        assertThat(result.getExchange()).isEqualTo("NASDAQ");
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getFigi()).isEqualTo("BBG000BPH459");
        verify(finnhubClient).searchSymbols("microsoft");
        verify(finnhubClient, org.mockito.Mockito.never()).lookupSymbol("microsoft");
    }
}
