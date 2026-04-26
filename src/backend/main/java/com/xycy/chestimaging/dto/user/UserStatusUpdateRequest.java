package com.xycy.chestimaging.dto.user;

import com.xycy.chestimaging.model.User;

public class UserStatusUpdateRequest {
    private User.Status status;

    public User.Status getStatus() {
        return status;
    }

    public void setStatus(User.Status status) {
        this.status = status;
    }
}