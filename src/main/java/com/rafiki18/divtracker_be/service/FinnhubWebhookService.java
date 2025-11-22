package com.rafiki18.divtracker_be.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FinnhubWebhookService {

    @Value("${finnhub.webhook-secret:}")
    private String webhookSecret;

    /**
     * Verifica que el secret del webhook coincida con el configurado
     */
    public boolean verifySecret(String receivedSecret) {
        // Si no hay secret configurado, rechazar
        if (!StringUtils.hasText(webhookSecret)) {
            log.warn("Finnhub webhook secret not configured");
            return false;
        }

        // Verificar que el secret recibido coincida
        if (!StringUtils.hasText(receivedSecret)) {
            log.warn("No webhook secret provided in request");
            return false;
        }

        return webhookSecret.equals(receivedSecret);
    }

    public boolean isWebhookEnabled() {
        return StringUtils.hasText(webhookSecret);
    }
}
