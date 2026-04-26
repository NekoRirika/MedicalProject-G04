package com.xycy.chestimaging.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private HttpStatus status;
    
    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }
    
    public HttpStatus getStatus() {
        return status;
    }
}
