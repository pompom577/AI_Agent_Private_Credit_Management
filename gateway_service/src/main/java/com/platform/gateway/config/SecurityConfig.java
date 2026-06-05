package com.platform.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS configuration updated to support React running on Port 3000.
 */
@Configuration
public class SecurityConfig {

    // 🔄 UPDATED: Added port 3000 alongside 5173 as a fallback default
    @Value("${gateway.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000,http://localhost:5173,http://127.0.0.1:5173}")
    private List<String> allowedOrigins;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(allowedOrigins);
        
        // 🔄 ALLOW ALL standard REST methods for flexibility
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 🔄 ALLOW ALL headers so multi-part file boundaries and JWT tokens aren't stripped
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setExposedHeaders(List.of("*"));
        
        // 🔄 SET TO TRUE so browser authentication headers pass safely
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        // 🔄 APPLY to all application paths (/**) instead of just /uploads to protect future APIs
        source.registerCorsConfiguration("/**", cfg);
        return new CorsFilter(source);
    }
}