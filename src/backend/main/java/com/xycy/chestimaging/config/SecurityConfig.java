package com.xycy.chestimaging.config;

import com.xycy.chestimaging.mapper.UserMapper;
import com.xycy.chestimaging.service.CacheService;
import com.xycy.chestimaging.utils.JwtUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtils jwtUtils, TokenBlackListUtil tokenBlackListUtil, UserMapper userMapper, CacheService cacheService) {
        return new JwtAuthenticationFilter(jwtUtils, tokenBlackListUtil, userMapper, cacheService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/users/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/audit-logs/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/cases/**").hasAnyAuthority("ROLE_DOCTOR","ROLE_RESEARCHER")
                .requestMatchers("/models/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_DOCTOR", "ROLE_RESEARCHER")
                .requestMatchers("/profile/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}