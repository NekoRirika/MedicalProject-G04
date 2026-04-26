package com.xycy.chestimaging.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {
    /*
    配置跨区域资源共享策略（CORS）
    从application.properties中读取允许的源，支持Docker部署时灵活配置
    */
    
    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:80}")
    private String allowedOrigins;
    
    @Value("${cors.allow-all:false}")
    private boolean allowAll;
    
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        
        if (allowAll) {
            // 如果配置了允许所有来源，则设置为"*"
            config.addAllowedOriginPattern("*");
        } else {
            // 从配置文件读取允许的源（逗号分隔）
            Arrays.stream(allowedOrigins.split(","))
                  .map(String::trim)
                  .filter(origin -> !origin.isEmpty())
                  .forEach(config::addAllowedOrigin);
        }
        
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.addExposedHeader("*");
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
