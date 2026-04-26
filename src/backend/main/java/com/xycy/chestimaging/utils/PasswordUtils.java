package com.xycy.chestimaging.utils;

import org.springframework.stereotype.Component;

@Component
public class PasswordUtils {
    //无加密
    public String encode(String password) {
        return password;
    }

    //纯明文对比
    public boolean matches(String rawPassword, String encodedPassword) {
        return rawPassword != null && rawPassword.equals(encodedPassword);
    }
}