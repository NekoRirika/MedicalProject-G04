package com.xycy.chestimaging.service.impl;

import com.xycy.chestimaging.config.TokenBlackListUtil;
import com.xycy.chestimaging.dto.user.LoginRequest;
import com.xycy.chestimaging.dto.user.LoginResponse;
import com.xycy.chestimaging.dto.user.UserResponse;
import com.xycy.chestimaging.mapper.UserMapper;
import com.xycy.chestimaging.mapper.UserTokenMapper;
import com.xycy.chestimaging.model.User;
import com.xycy.chestimaging.model.UserToken;
import com.xycy.chestimaging.service.AuthService;
import com.xycy.chestimaging.service.CacheService;
import com.xycy.chestimaging.utils.JwtUtils;
import com.xycy.chestimaging.utils.PasswordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private PasswordUtils passwordUtils;
    @Autowired
    private TokenBlackListUtil tokenBlackListUtil;
    @Autowired
    private UserTokenMapper userTokenMapper;
    @Autowired
    private CacheService cacheService;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 校验用户名密码
        Optional<User> optionalUser = userMapper.findByUsername(request.getUsername());
        if (!optionalUser.isPresent()) {
            throw new RuntimeException("用户不存在");
        }

        User user = optionalUser.get();

        if (user.getStatus() == User.Status.locked) {
            throw new RuntimeException("用户已被锁定");
        }

        boolean passwordMatch = passwordUtils.matches(request.getPassword(), user.getPassword());
        if (!passwordMatch) {
            throw new RuntimeException("密码错误");
        }

        // 更新用户登录时间
        user.setLastLogin(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        userMapper.update(user);

        // 2. 删除该用户所有旧 Token
        List<UserToken> allTokens = userTokenMapper.findTokensByUsername(user.getUsername());
        for (UserToken userToken : allTokens) {
            String oldToken = userToken.getToken();
            try {
                // 尝试获取过期时间，加入黑名单
                long expirationTime = jwtUtils.getExpirationFromToken(oldToken);
                tokenBlackListUtil.addToBlacklist(oldToken, expirationTime);
            } catch (Exception e) {
                // Token可能已经无效，继续处理
            }
            // 从数据库删除旧Token
            userTokenMapper.deleteById(userToken.getId());
        }

        // 3. 用当前新密钥 生成全新 Token
        String token = jwtUtils.generateToken(user.getUsername());
        
        // 4. 把新 Token 存入数据库
        UserToken newUserToken = new UserToken();
        newUserToken.setUsername(user.getUsername());
        newUserToken.setToken(token);
        newUserToken.setCreatedAt(LocalDateTime.now());
        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpiration * 1000);
        newUserToken.setExpiresAt(LocalDateTime.ofInstant(expiryDate.toInstant(), ZoneId.systemDefault()));
        userTokenMapper.insert(newUserToken);

        // 5. 将会话信息写入Redis缓存
        cacheService.cacheUserSession(token, user.getUsername(), user.getRole().name(), user.getDepartment());
        logger.info("[登录] 用户{}登录成功，会话已缓存至Redis", user.getUsername());

        // 6. 返回新 Token 给前端
        return new LoginResponse(token, new UserResponse(user));
    }

    @Override
    public void logout(String username, String token) {
        // 将token加入黑名单
        long expirationTime = jwtUtils.getExpirationFromToken(token);
        tokenBlackListUtil.addToBlacklist(token, expirationTime);
        
        // 从Redis删除会话
        cacheService.evictUserSession(token);
        logger.info("[登出] 用户{}登出，Redis会话已删除", username);
    }
}