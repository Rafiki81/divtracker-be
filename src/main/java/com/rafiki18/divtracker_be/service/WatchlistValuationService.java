package com.rafiki18.divtracker_be.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.rafiki18.divtracker_be.dto.WatchlistItemResponse;
import com.rafiki18.divtracker_be.marketdata.FinnhubClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchlistValuationService {

    private static final int SCALE = 4;

    private final FinnhubClient finnhubClient;

    public WatchlistItemResponse enrich(WatchlistItemResponse response) {
        if (response == null || !StringUtils.hasText(response.getTicker()) || !finnhubClient.isEnabled()) {
            return response;
        }

        String ticker = response.getTicker();

        Optional<BigDecimal> currentPrice = finnhubClient.fetchCurrentPrice(ticker);
        Optional<BigDecimal> fcfPerShare = finnhubClient.calculateFCF(ticker)
                .map(fcfData -> fcfData.get("fcfPerShare"));

        currentPrice.ifPresent(response::setCurrentPrice);
        fcfPerShare.ifPresent(response::setFreeCashFlowPerShare);

        if (currentPrice.isPresent() && fcfPerShare.isPresent() && fcfPerShare.get().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal actualPfcf = currentPrice.get().divide(fcfPerShare.get(), SCALE, RoundingMode.HALF_UP);
            response.setActualPfcf(actualPfcf);
        }

        if (response.getTargetPfcf() != null && fcfPerShare.isPresent()) {
            BigDecimal fairPrice = fcfPerShare.get().multiply(response.getTargetPfcf()).setScale(SCALE, RoundingMode.HALF_UP);
            response.setFairPriceByPfcf(fairPrice);

            currentPrice.ifPresent(price -> {
                if (fairPrice.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal discount = fairPrice.subtract(price)
                            .divide(fairPrice, SCALE, RoundingMode.HALF_UP);
                    response.setDiscountToFairPrice(discount);
                }
                response.setUndervalued(price.compareTo(fairPrice) < 0);
            });
        }

        if (response.getTargetPrice() != null
                && response.getTargetPrice().compareTo(BigDecimal.ZERO) > 0
                && currentPrice.isPresent()) {
            BigDecimal priceGap = currentPrice.get().subtract(response.getTargetPrice())
                    .divide(response.getTargetPrice(), SCALE, RoundingMode.HALF_UP);
            response.setDeviationFromTargetPrice(priceGap);
        }

        return response;
    }
}
