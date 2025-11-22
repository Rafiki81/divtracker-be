package com.rafiki18.divtracker_be.marketdata.stream;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubTradeData(
        @JsonProperty("s") String symbol,
        @JsonProperty("p") BigDecimal price,
        @JsonProperty("v") BigDecimal volume,
        @JsonProperty("t") Long timestamp
) {
}
