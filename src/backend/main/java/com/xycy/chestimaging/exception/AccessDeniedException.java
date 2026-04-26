package com.xycy.chestimaging.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedException extends RuntimeException {
    private HttpStatus status;
    
    public AccessDeniedException(String message) {
        super(message);
        this.status = HttpStatus.FORBIDDEN;
    }
    
    public HttpStatus getStatus() {
        return status;
    }
}
