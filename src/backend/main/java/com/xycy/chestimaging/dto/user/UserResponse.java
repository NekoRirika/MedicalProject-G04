package com.xycy.chestimaging.dto.user;

import com.xycy.chestimaging.model.User;

import java.time.LocalDateTime;

public class UserResponse {
    private Long id;
    private String username;
    private String name;
    private String department;
    private User.Role role;
    private User.Status status;
    private LocalDateTime createdTime;
    private LocalDateTime lastLogin;

    public UserResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.name = user.getName();
        this.department = user.getDepartment();
        this.role = user.getRole();
        this.status = user.getStatus();
        this.createdTime = user.getCreatedTime();
        this.lastLogin = user.getLastLogin();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public User.Role getRole() {
        return role;
    }

    public void setRole(User.Role role) {
        this.role = role;
    }

    public User.Status getStatus() {
        return status;
    }

    public void setStatus(User.Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
}