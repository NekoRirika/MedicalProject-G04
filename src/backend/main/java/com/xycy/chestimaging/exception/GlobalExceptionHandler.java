package com.xycy.chestimaging.exception;

import com.xycy.chestimaging.dto.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response<?>> handleAccessDeniedException(AccessDeniedException ex) {
        Response<?> response = Response.error(403, "权限不足", ex.getMessage());
        return new ResponseEntity<>(response, ex.getStatus());
    }
    
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Response<?>> handleNotFoundException(NotFoundException ex) {
        Response<?> response = Response.error(404, "资源不存在", ex.getMessage());
        return new ResponseEntity<>(response, ex.getStatus());
    }
    
    @ExceptionHandler(FileTypeException.class)
    public ResponseEntity<Response<?>> handleFileTypeException(FileTypeException ex) {
        Response<?> response = Response.error(400, "文件类型错误", ex.getMessage());
        return new ResponseEntity<>(response, ex.getStatus());
    }
    
    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<Response<?>> handleUsernameAlreadyExistsException(UsernameAlreadyExistsException ex) {
        Response<?> response = Response.error(400, "用户名已存在", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Response<?>> handleBusinessException(BusinessException ex) {
        Response<?> response = Response.error(400, "业务校验失败", ex.getMessage());
        return new ResponseEntity<>(response, ex.getStatus());
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Response<?>> handleRuntimeException(RuntimeException ex) {
        Response<?> response = Response.error(500, "服务器内部错误", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
