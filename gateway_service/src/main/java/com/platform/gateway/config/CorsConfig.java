package com.platform.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // Allows all paths in your application
                        .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000") // Allows your React frontend
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allows all necessary actions
                        .allowedHeaders("*") // Allows all headers (like your JWT tokens!)
                        .allowCredentials(true);
            }
        };
    }
}