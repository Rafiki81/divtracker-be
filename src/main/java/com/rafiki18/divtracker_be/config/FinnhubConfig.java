package com.rafiki18.divtracker_be.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.rafiki18.divtracker_be.config.properties.FinnhubProperties;

@Configuration
@EnableConfigurationProperties(FinnhubProperties.class)
public class FinnhubConfig {

    @Bean
    WebClient finnhubWebClient(FinnhubProperties properties) {
        return WebClient.builder()
            .baseUrl(properties.getApiUrl())
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(256 * 1024))
                        .build())
                .build();
    }
}
