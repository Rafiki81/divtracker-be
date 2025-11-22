package com.rafiki18.divtracker_be.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "finnhub")
public class FinnhubProperties {

    private String apiUrl = "https://finnhub.io/api/v1";
    private String apiKey;
    private String websocketUrl = "wss://ws.finnhub.io";
    private Duration streamReconnectDelay = Duration.ofSeconds(5);
    private boolean streamEnabled = true;
    private String webhookSecret;

    public boolean isEnabled() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(apiUrl);
    }

    public boolean isStreamingEnabled() {
        return streamEnabled && isEnabled() && StringUtils.hasText(websocketUrl);
    }
    
    public boolean isWebhookEnabled() {
        return StringUtils.hasText(webhookSecret);
    }
}
