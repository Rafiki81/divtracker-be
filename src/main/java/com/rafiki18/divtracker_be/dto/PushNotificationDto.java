package com.rafiki18.divtracker_be.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for push notification payloads.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushNotificationDto {

    private PushNotificationType type;
    private String title;
    private String body;
    
    @Builder.Default
    private Map<String, String> data = new HashMap<>();

    /**
     * Create a PRICE_UPDATE notification (data-only, silent).
     */
    public static PushNotificationDto priceUpdate(String ticker, BigDecimal price, BigDecimal changePercent) {
        Map<String, String> data = new HashMap<>();
        data.put("type", PushNotificationType.PRICE_UPDATE.name());
        data.put("ticker", ticker);
        data.put("price", price.toPlainString());
        if (changePercent != null) {
            data.put("changePercent", changePercent.toPlainString());
        }
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return PushNotificationDto.builder()
                .type(PushNotificationType.PRICE_UPDATE)
                .data(data)
                .build();
    }

    /**
     * Create a PRICE_ALERT notification (visible).
     */
    public static PushNotificationDto priceAlert(String ticker, BigDecimal currentPrice, BigDecimal targetPrice) {
        String direction = currentPrice.compareTo(targetPrice) >= 0 ? "alcanzado" : "por debajo de";
        
        Map<String, String> data = new HashMap<>();
        data.put("type", PushNotificationType.PRICE_ALERT.name());
        data.put("ticker", ticker);
        data.put("currentPrice", currentPrice.toPlainString());
        data.put("targetPrice", targetPrice.toPlainString());
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return PushNotificationDto.builder()
                .type(PushNotificationType.PRICE_ALERT)
                .title("🎯 Alerta de Precio: " + ticker)
                .body(String.format("%s ha %s tu precio objetivo ($%s → $%s)", 
                    ticker, direction, targetPrice.toPlainString(), currentPrice.toPlainString()))
                .data(data)
                .build();
    }

    /**
     * Create a MARGIN_ALERT notification (visible).
     */
    public static PushNotificationDto marginAlert(String ticker, BigDecimal marginOfSafety, BigDecimal currentPrice) {
        Map<String, String> data = new HashMap<>();
        data.put("type", PushNotificationType.MARGIN_ALERT.name());
        data.put("ticker", ticker);
        data.put("marginOfSafety", marginOfSafety.toPlainString());
        data.put("currentPrice", currentPrice.toPlainString());
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return PushNotificationDto.builder()
                .type(PushNotificationType.MARGIN_ALERT)
                .title("📈 Oportunidad: " + ticker)
                .body(String.format("%s tiene un margen de seguridad del %.1f%% - ¡Posible oportunidad de compra!", 
                    ticker, marginOfSafety.doubleValue()))
                .data(data)
                .build();
    }

    /**
     * Create a DAILY_SUMMARY notification (visible).
     */
    public static PushNotificationDto dailySummary(List<String> tickers, int gainersCount, int losersCount) {
        Map<String, String> data = new HashMap<>();
        data.put("type", PushNotificationType.DAILY_SUMMARY.name());
        data.put("tickerCount", String.valueOf(tickers.size()));
        data.put("gainersCount", String.valueOf(gainersCount));
        data.put("losersCount", String.valueOf(losersCount));
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));

        String summary;
        if (gainersCount > losersCount) {
            summary = String.format("📈 %d subiendo, %d bajando", gainersCount, losersCount);
        } else if (losersCount > gainersCount) {
            summary = String.format("📉 %d bajando, %d subiendo", losersCount, gainersCount);
        } else {
            summary = String.format("➡️ %d subiendo, %d bajando", gainersCount, losersCount);
        }

        return PushNotificationDto.builder()
                .type(PushNotificationType.DAILY_SUMMARY)
                .title("📊 Resumen Diario - DivTracker")
                .body(String.format("Tu watchlist (%d acciones): %s", tickers.size(), summary))
                .data(data)
                .build();
    }

    /**
     * Check if this is a data-only (silent) notification.
     */
    public boolean isDataOnly() {
        return type == PushNotificationType.PRICE_UPDATE;
    }
}
