package com.rafiki18.divtracker_be.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Value("${app.name:DivTracker}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.description:Backend para seguimiento y valoración de acciones con análisis de Free Cash Flow, DCF, TIR y métricas financieras avanzadas}")
    private String appDescription;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .info(new Info()
                        .title(appName + " API")
                        .version(appVersion)
                        .description(appDescription + "\n\n" +
                                "### Características principales:\n" +
                                "- 📊 **Análisis de valoración**: Cálculo de precio justo usando P/FCF objetivo\n" +
                                "- 💰 **DCF (Discounted Cash Flow)**: Valoración intrínseca con flujos descontados\n" +
                                "- 📈 **TIR (Tasa Interna de Retorno)**: Rentabilidad esperada de la inversión\n" +
                                "- 🎯 **FCF Yield**: Rendimiento del Free Cash Flow\n" +
                                "- 🛡️ **Margen de seguridad**: Diferencia entre precio actual y valor intrínseco\n" +
                                "- ⏱️ **Payback Period**: Tiempo estimado de recuperación de la inversión\n" +
                                "- 📊 **ROI estimado**: Retorno esperado según el horizonte de inversión\n" +
                                "- 🔔 **Alertas de precio**: Notificaciones cuando el precio alcanza objetivos")
                        .contact(new Contact()
                                .name("Rafael Perez Beato")
                                .email("rafiki18@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor de Desarrollo"),
                        new Server()
                                .url("https://api.divtracker.com")
                                .description("Servidor de Producción")))
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingrese el token JWT obtenido del endpoint de login")));
    }
}
