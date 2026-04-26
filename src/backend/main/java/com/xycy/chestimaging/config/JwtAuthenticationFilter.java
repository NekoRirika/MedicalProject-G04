package com.xycy.chestimaging.config;

import com.xycy.chestimaging.mapper.UserMapper;
import com.xycy.chestimaging.model.User;
import com.xycy.chestimaging.service.CacheService;
import com.xycy.chestimaging.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;

/*实现JWT验证过滤器，用于验证每个HTTP请求的身份*/

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final JwtUtils jwtUtils;
    private final TokenBlackListUtil tokenBlackListUtil;
    private final UserMapper userMapper;
    private final CacheService cacheService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, TokenBlackListUtil tokenBlackListUtil, UserMapper userMapper, CacheService cacheService) {
        this.jwtUtils = jwtUtils;
        this.tokenBlackListUtil = tokenBlackListUtil;
        this.userMapper = userMapper;
        this.cacheService = cacheService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/api/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write("{\"code\":401,\"message\":\"缺少 token\",\"data\":null}");
            writer.flush();
            return;
        }

        String token = authorizationHeader.substring(7);

        if (tokenBlackListUtil.isTokenBlacklisted(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write("{\"code\":401,\"message\":\"token 已失效\",\"data\":null}");
            writer.flush();
            return;
        }

        if (jwtUtils.validateToken(token)) {
            String username = jwtUtils.getUsernameFromToken(token);
            
            User user = null;
            
            String sessionData = cacheService.getUserSession(token);
            if (sessionData != null) {
                logger.debug("[Token验证] 从Redis获取会话信息: token前缀={}", token.substring(0, Math.min(8, token.length())));
                String[] parts = sessionData.split("\\|");
                if (parts.length >= 3) {
                    user = userMapper.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("用户不存在"));
                }
            } else {
                logger.debug("[Token验证] Redis未命中，从数据库查询: username={}", username);
                user = userMapper.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("用户不存在"));
            }

            UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name().toUpperCase())))
                    .build();

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write("{\"code\":401,\"message\":\"token 无效\",\"data\":null}");
            writer.flush();
            return;
        }

        filterChain.doFilter(request, response);
    }
}