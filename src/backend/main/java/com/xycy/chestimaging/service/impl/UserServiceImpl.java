package com.xycy.chestimaging.service.impl;

import com.xycy.chestimaging.dto.PaginationResponse;
import com.xycy.chestimaging.dto.user.UserCreateRequest;
import com.xycy.chestimaging.dto.user.UserResponse;
import com.xycy.chestimaging.dto.user.UserUpdateRequest;
import com.xycy.chestimaging.exception.BusinessException;
import com.xycy.chestimaging.exception.UsernameAlreadyExistsException;
import com.xycy.chestimaging.mapper.UserMapper;
import com.xycy.chestimaging.model.User;
import com.xycy.chestimaging.service.CacheService;
import com.xycy.chestimaging.service.UserService;
import com.xycy.chestimaging.utils.PasswordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    
    private static final List<String> DOCTOR_DEPARTMENTS = Arrays.asList(
            "心内科", "呼吸科", "消化科", "神经内科", "神经外科",
            "骨科", "普外科", "胸外科", "妇产科", "儿科",
            "内分泌科", "肾内科", "血液科", "肿瘤科", "感染科",
            "风湿免疫科", "急诊科", "重症医学科", "放射科"
    );

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PasswordUtils passwordUtils;
    @Autowired
    private CacheService cacheService;

    @Override
    public PaginationResponse<UserResponse> getUsers(int page, int pageSize, String username, String department) {
        int offset = (page - 1) * pageSize;
        String role = null;
        String status = null;
        
        List<User> users = userMapper.findByCondition(username, department, role, status, offset, pageSize);
        long total = userMapper.countByCondition(username, department, role, status);
        
        List<UserResponse> userResponses = users.stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());

        return new PaginationResponse<>(total, userResponses);
    }

    @Override
    public UserResponse createUser(UserCreateRequest request) {
        if (userMapper.findByUsername(request.getUsername()).isPresent()) {
            throw new UsernameAlreadyExistsException("用户名 '" + request.getUsername() + "' 已被注册，请选择其他用户名");
        }

        validateDepartmentRole(request.getDepartment(), request.getRole());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setDepartment(request.getDepartment());
        user.setRole(request.getRole());
        user.setPassword(passwordUtils.encode(request.getPassword()));
        user.setStatus(User.Status.active);
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());

        userMapper.insert(user);
        return new UserResponse(user);
    }

    private void validateDepartmentRole(String department, User.Role role) {
        if ("管理部".equals(department)) {
            if (role != User.Role.admin) {
                throw new BusinessException("管理部只能创建管理员角色账号");
            }
        } else if ("科研部".equals(department)) {
            if (role != User.Role.researcher) {
                throw new BusinessException("科研部只能创建科研人员角色账号");
            }
        } else if (DOCTOR_DEPARTMENTS.contains(department)) {
            if (role != User.Role.doctor) {
                throw new BusinessException("医生部门只能创建医生角色账号");
            }
        } else {
            throw new BusinessException("无效的部门选择: " + department);
        }
    }

    @Override
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        Optional<User> optionalUser = userMapper.findById(id);
        if (!optionalUser.isPresent()) {
            throw new RuntimeException("用户不存在");
        }

        User user = optionalUser.get();
        
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userMapper.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("当前用户不存在"));
        
        if (currentUser.getId().equals(user.getId())) {
            if (request.getName() != null && !request.getName().equals(user.getName())) {
                throw new BusinessException("不能修改自己的姓名");
            }
            if (request.getRole() != null && !request.getRole().equals(user.getRole())) {
                throw new BusinessException("不能修改自己的角色");
            }
        }
        
        validateDepartmentRole(request.getDepartment(), request.getRole());
        
        user.setName(request.getName());
        user.setDepartment(request.getDepartment());
        user.setRole(request.getRole());
        user.setUpdatedTime(LocalDateTime.now());

        userMapper.update(user);
        
        cacheService.evictUserInfo(user.getUsername());
        logger.info("[用户更新] 用户信息已更新，缓存已删除: {}", user.getUsername());
        
        return new UserResponse(user);
    }

    @Override
    public void deleteUser(Long id) {
        Optional<User> optionalUser = userMapper.findById(id);
        if (!optionalUser.isPresent()) {
            throw new RuntimeException("用户不存在");
        }
        userMapper.deleteById(id);
    }

    @Override
    public String resetPassword(Long id) {
        Optional<User> optionalUser = userMapper.findById(id);
        if (!optionalUser.isPresent()) {
            throw new RuntimeException("用户不存在");
        }

        User user = optionalUser.get();
        String newPassword = "123456";
        user.setPassword(passwordUtils.encode(newPassword));
        user.setUpdatedTime(LocalDateTime.now());
        userMapper.update(user);

        return newPassword;
    }

    @Override
    public UserResponse updateUserStatus(Long id, User.Status status) {
        Optional<User> optionalUser = userMapper.findById(id);
        if (!optionalUser.isPresent()) {
            throw new RuntimeException("用户不存在");
        }

        User user = optionalUser.get();
        user.setStatus(status);
        user.setUpdatedTime(LocalDateTime.now());
        userMapper.update(user);
        return new UserResponse(user);
    }

    @Override
    public User findByUsername(String username) {
        User cachedUser = cacheService.getUserInfo(username);
        if (cachedUser != null) {
            logger.info("[用户查询] 从Redis缓存获取用户信息: {}", username);
            return cachedUser;
        }
        
        logger.info("[用户查询] 缓存未命中，从数据库查询: {}", username);
        Optional<User> optionalUser = userMapper.findByUsername(username);
        if (!optionalUser.isPresent()) {
            throw new RuntimeException("用户不存在");
        }
        
        User user = optionalUser.get();
        cacheService.cacheUserInfo(username, user);
        return user;
    }
}
