package com.xycy.chestimaging.config;

import com.xycy.chestimaging.mapper.UserTokenMapper;
import com.xycy.chestimaging.model.UserToken;
import com.xycy.chestimaging.utils.JwtUtils;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenBlackListUtil {
    private static final Logger logger = LoggerFactory.getLogger(TokenBlackListUtil.class);
    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private UserTokenMapper userTokenMapper;
    
    @Scheduled(fixedRate = 300000)
    public void cleanExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        Set<String> expiredTokens = new HashSet<>();
        
        for (String token : blacklist.keySet()) {
            Long expirationTime = blacklist.get(token);
            if (expirationTime < currentTime) {
                expiredTokens.add(token);
            }
        }
        
        for (String token : expiredTokens) {
            blacklist.remove(token);
        }
    }
    
    public void addToBlacklist(String token, long expirationTime) {
        blacklist.put(token, expirationTime);
    }
    
    public boolean isTokenBlacklisted(String token) {
        cleanExpiredTokens();
        return blacklist.containsKey(token);
    }
    
    @PreDestroy
    public void invalidateAllTokensOnShutdown() {
        logger.info("应用关闭中，将所有活跃 token 加入黑名单...");
        List<UserToken> activeTokens = userTokenMapper.findAllActiveTokens();
        long currentTime = System.currentTimeMillis();
        
        for (UserToken userToken : activeTokens) {
            try {
                long expirationTime = jwtUtils.getExpirationFromToken(userToken.getToken());
                blacklist.put(userToken.getToken(), expirationTime);
            } catch (Exception e) {
                logger.warn("Token 无效，跳过：{}", userToken.getToken());
            }
        }
        
        logger.info("已将所有 {} 个 token 加入黑名单", blacklist.size());
    }
}
