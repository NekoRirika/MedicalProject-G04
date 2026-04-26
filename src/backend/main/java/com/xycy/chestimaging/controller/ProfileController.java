package com.xycy.chestimaging.controller;

import com.xycy.chestimaging.dto.Response;
import com.xycy.chestimaging.dto.user.PasswordUpdateRequest;
import com.xycy.chestimaging.dto.user.UserResponse;
import com.xycy.chestimaging.mapper.UserMapper;
import com.xycy.chestimaging.model.User;
import com.xycy.chestimaging.utils.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/profile")
public class ProfileController {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordUtils passwordUtils;

    @GetMapping
    public Response<?> getProfile() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userMapper.findByUsername(username).orElseThrow(() -> new RuntimeException("用户不存在"));
            return Response.success("查询成功", new UserResponse(user));
        } catch (RuntimeException e) {
            return Response.error(500, "查询失败", e.getMessage());
        }
    }

    @PutMapping("/password")
    public Response<?> updatePassword(@RequestBody PasswordUpdateRequest request) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userMapper.findByUsername(username).orElseThrow(() -> new RuntimeException("用户不存在"));

            // 验证原密码
            if (!passwordUtils.matches(request.getOldPassword(), user.getPassword())) {
                return Response.error(400, "密码修改失败", "原密码错误");
            }

            // 验证原密码和新密码不能相同
            if (request.getOldPassword().equals(request.getNewPassword())) {
                return Response.error(400, "密码修改失败", "新密码不能与原密码相同");
            }

            // 验证新密码和确认密码是否一致
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return Response.error(400, "密码修改失败", "新密码和确认密码不一致");
            }

            // 更新密码
            user.setPassword(passwordUtils.encode(request.getNewPassword()));
            user.setUpdatedTime(LocalDateTime.now());
            userMapper.update(user);

            return Response.success("密码修改成功");
        } catch (RuntimeException e) {
            return Response.error(400, "密码修改失败", e.getMessage());
        }
    }
}