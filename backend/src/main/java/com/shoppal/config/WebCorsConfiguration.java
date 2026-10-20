package com.shoppal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfiguration implements WebMvcConfigurer {
    @Override public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                .allowedMethods("GET", "POST", "OPTIONS", "HEAD", "DELETE")
                .allowedHeaders("Content-Type", "Authorization", "X-Requested-With", "Accept", "Origin", "Mcp-Session-Id", "mcp-session-id")
                .exposedHeaders("Mcp-Session-Id", "mcp-session-id")
                .maxAge(3600);
    }
}
