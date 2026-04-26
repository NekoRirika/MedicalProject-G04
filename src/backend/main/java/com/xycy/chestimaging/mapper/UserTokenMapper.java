package com.xycy.chestimaging.mapper;

import com.xycy.chestimaging.model.UserToken;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface UserTokenMapper {
    int insert(UserToken userToken);
    List<UserToken> findActiveTokensByUsername(String username);
    List<UserToken> findAllActiveTokens();
    List<UserToken> findTokensByUsername(String username);
    int invalidateToken(Long id);
    int deleteById(Long id);
    int deleteExpiredTokens();
}