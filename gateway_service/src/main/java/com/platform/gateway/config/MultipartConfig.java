package com.platform.gateway.config;

import org.springframework.context.annotation.Configuration;

/**
 * TODO(1.1a): Tune Tomcat / Spring multipart limits beyond what application.yml exposes if needed
 * (e.g., file-size-threshold, temp location). For the 500 MB cap, application.yml is sufficient.
 */
@Configuration
public class MultipartConfig {
    // Intentionally empty — placeholder for future programmatic multipart tuning.
}
