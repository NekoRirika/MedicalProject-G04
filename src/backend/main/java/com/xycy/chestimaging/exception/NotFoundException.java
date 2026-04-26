package com.xycy.chestimaging.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends RuntimeException {
    private HttpStatus status;
    
    public NotFoundException(String message) {
        super(message);
        this.status = HttpStatus.NOT_FOUND;
    }
    
    public HttpStatus getStatus() {
        return status;
    }
}
