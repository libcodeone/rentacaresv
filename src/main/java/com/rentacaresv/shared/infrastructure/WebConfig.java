package com.rentacaresv.shared.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Configuración CORS para la API pública.
 * Permite que el frontend (novarentacarsv.com) consuma los endpoints REST.
 */
@Configuration
public class WebConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Orígenes permitidos
        config.setAllowedOrigins(List.of(
                "https://novarentacarsv.com",
                "https://www.novarentacarsv.com",
                "https://adm.novarentacares.com",
                "http://localhost:3000",   // Next.js dev
                "http://localhost:5173"    // Vite dev
        ));

        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Solo aplicar CORS a la API pública
        source.registerCorsConfiguration("/api/public/**", config);

        return new CorsFilter(source);
    }
}
