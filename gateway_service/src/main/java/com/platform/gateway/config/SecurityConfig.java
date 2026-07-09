package com.platform.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS configuration updated to dynamically support deployment on any AWS Public IP address.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();

        // while keeping setAllowCredentials(true) active!
        cfg.setAllowedOriginPatterns(List.of("*"));
        
        // 🔄 ALLOW ALL standard REST methods for flexibility
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 🔄 ALLOW ALL headers so multi-part file boundaries and JWT tokens aren't stripped
        cfg.setAllowedHeaders(List.of("*"));
        
        // 🔄 EXPOSE ALL headers to the client frontend response context
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