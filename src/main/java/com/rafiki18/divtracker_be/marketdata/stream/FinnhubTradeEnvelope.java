package com.rafiki18.divtracker_be.marketdata.stream;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubTradeEnvelope(
        @JsonProperty("type") String type,
        @JsonProperty("data") List<FinnhubTradeData> data
) {
}
