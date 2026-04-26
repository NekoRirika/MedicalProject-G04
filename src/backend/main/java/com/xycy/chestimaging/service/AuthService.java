package com.xycy.chestimaging.service;

import com.xycy.chestimaging.dto.user.LoginRequest;
import com.xycy.chestimaging.dto.user.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    void logout(String username, String token);
}
