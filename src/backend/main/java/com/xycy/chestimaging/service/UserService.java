package com.xycy.chestimaging.service;

import com.xycy.chestimaging.dto.PaginationResponse;
import com.xycy.chestimaging.dto.user.UserCreateRequest;
import com.xycy.chestimaging.dto.user.UserResponse;
import com.xycy.chestimaging.dto.user.UserUpdateRequest;
import com.xycy.chestimaging.model.User;

public interface UserService {
    PaginationResponse<UserResponse> getUsers(int page, int pageSize, String username, String department);
    UserResponse createUser(UserCreateRequest request);
    UserResponse updateUser(Long id, UserUpdateRequest request);
    void deleteUser(Long id);
    String resetPassword(Long id);
    UserResponse updateUserStatus(Long id, User.Status status);
    User findByUsername(String username);
}
