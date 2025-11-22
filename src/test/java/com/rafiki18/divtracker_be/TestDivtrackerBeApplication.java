package com.rafiki18.divtracker_be;

import org.springframework.boot.SpringApplication;

import com.rafiki18.divtracker_be.config.TestcontainersConfiguration;

/**
 * Clase principal para ejecutar la aplicación en modo desarrollo local con Testcontainers.
 * 
 * Esta clase levanta automáticamente un contenedor Docker de PostgreSQL
 * cuando ejecutas la aplicación, sin necesidad de tener PostgreSQL instalado localmente.
 * 
 * CÓMO USAR:
 * 
 * Opción 1 - Desde el IDE:
 *   - Ejecuta esta clase directamente desde tu IDE (IntelliJ, Eclipse, VS Code)
 *   - El contenedor PostgreSQL se levantará automáticamente
 * 
 * Opción 2 - Desde Maven:
 *   mvn spring-boot:test-run
 * 
 * Opción 3 - Desde línea de comandos:
 *   mvn compile
 *   mvn exec:java -Dexec.mainClass="com.rafiki18.divtracker_be.TestDivtrackerBeApplication"
 * 
 * REQUISITOS:
 *   - Docker debe estar instalado y en ejecución
 *   - Puerto 8080 debe estar disponible para la aplicación
 *   - Testcontainers descargará la imagen de PostgreSQL automáticamente
 * 
 * VENTAJAS:
 *   - No necesitas instalar PostgreSQL localmente
 *   - Base de datos limpia en cada ejecución (o reutilizable con withReuse(true))
 *   - Mismo entorno en todos los desarrolladores
 *   - Flyway ejecuta las migraciones automáticamente
 * 
 * La aplicación estará disponible en:
 *   - API: http://localhost:8080
 *   - Swagger: http://localhost:8080/swagger-ui.html
 *   - API Docs: http://localhost:8080/api-docs
 */
public class TestDivtrackerBeApplication {

    public static void main(String[] args) {
        // Configura el perfil 'local' automáticamente
        System.setProperty("spring.profiles.active", "local");
        
        SpringApplication
            .from(DivtrackerBeApplication::main)
            .with(TestcontainersConfiguration.class)
            .run(args);
    }
}
