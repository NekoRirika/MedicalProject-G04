package com.xycy.chestimaging.dto.user;

import com.xycy.chestimaging.model.User;

public class UserUpdateRequest {
    private String name;
    private String department;
    private User.Role role;

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
}