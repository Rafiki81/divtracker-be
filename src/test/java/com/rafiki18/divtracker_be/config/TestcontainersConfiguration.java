package com.rafiki18.divtracker_be.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Configuración de Testcontainers para desarrollo local.
 * Esta clase configura un contenedor PostgreSQL que se levanta automáticamente
 * cuando ejecutas la aplicación en modo desarrollo.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("divtracker")
                .withUsername("divtracker_user")
                .withPassword("divtracker_pass")
                .withReuse(true); // Reutiliza el contenedor entre ejecuciones
    }
}
