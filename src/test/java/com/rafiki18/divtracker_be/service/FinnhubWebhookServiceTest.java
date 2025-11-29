package com.rafiki18.divtracker_be.service;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FinnhubWebhookService Tests")
class FinnhubWebhookServiceTest {

    private FinnhubWebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new FinnhubWebhookService();
    }

    private void setWebhookSecret(String secret) throws Exception {
        Field field = FinnhubWebhookService.class.getDeclaredField("webhookSecret");
        field.setAccessible(true);
        field.set(webhookService, secret);
    }

    @Nested
    @DisplayName("When webhook secret is configured")
    class WithConfiguredSecret {

        @BeforeEach
        void setUp() throws Exception {
            setWebhookSecret("my-secret-key");
        }

        @Test
        @DisplayName("verifySecret() - should return true for matching secret")
        void verifySecret_matchingSecret_returnsTrue() {
            assertThat(webhookService.verifySecret("my-secret-key")).isTrue();
        }

        @Test
        @DisplayName("verifySecret() - should return false for wrong secret")
        void verifySecret_wrongSecret_returnsFalse() {
            assertThat(webhookService.verifySecret("wrong-secret")).isFalse();
        }

        @Test
        @DisplayName("verifySecret() - should return false for null secret")
        void verifySecret_nullSecret_returnsFalse() {
            assertThat(webhookService.verifySecret(null)).isFalse();
        }

        @Test
        @DisplayName("verifySecret() - should return false for empty secret")
        void verifySecret_emptySecret_returnsFalse() {
            assertThat(webhookService.verifySecret("")).isFalse();
        }

        @Test
        @DisplayName("isWebhookEnabled() - should return true when secret is configured")
        void isWebhookEnabled_secretConfigured_returnsTrue() {
            assertThat(webhookService.isWebhookEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("When webhook secret is not configured")
    class WithoutConfiguredSecret {

        @BeforeEach
        void setUp() throws Exception {
            setWebhookSecret("");
        }

        @Test
        @DisplayName("verifySecret() - should return false when no secret configured")
        void verifySecret_noSecretConfigured_returnsFalse() {
            assertThat(webhookService.verifySecret("any-secret")).isFalse();
        }

        @Test
        @DisplayName("isWebhookEnabled() - should return false when no secret configured")
        void isWebhookEnabled_noSecretConfigured_returnsFalse() {
            assertThat(webhookService.isWebhookEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("When webhook secret is null")
    class WithNullSecret {

        @BeforeEach
        void setUp() throws Exception {
            setWebhookSecret(null);
        }

        @Test
        @DisplayName("verifySecret() - should return false when secret is null")
        void verifySecret_nullConfiguredSecret_returnsFalse() {
            assertThat(webhookService.verifySecret("any-secret")).isFalse();
        }

        @Test
        @DisplayName("isWebhookEnabled() - should return false when secret is null")
        void isWebhookEnabled_nullSecret_returnsFalse() {
            assertThat(webhookService.isWebhookEnabled()).isFalse();
        }
    }
}
