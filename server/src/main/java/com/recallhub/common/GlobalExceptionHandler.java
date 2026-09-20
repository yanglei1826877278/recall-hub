package com.recallhub.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<?> business(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of(
                "code", ex.getCode(), "message", ex.getMessage(), "requestId", requestId()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    ResponseEntity<?> validation(Exception ex) {
        String message = ex instanceof MethodArgumentNotValidException manv
                ? manv.getBindingResult().getFieldErrors().stream().findFirst()
                    .map(e -> e.getField() + ": " + e.getDefaultMessage()).orElse("请求参数无效")
                : ex.getMessage();
        return ResponseEntity.badRequest().body(Map.of(
                "code", "VALIDATION_ERROR", "message", message, "requestId", requestId()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<?> denied(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(Map.of(
                "code", "FORBIDDEN", "message", "没有权限执行此操作", "requestId", requestId()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception ex) {
        log.error("Unhandled request error", ex);
        return ResponseEntity.internalServerError().body(Map.of(
                "code", "INTERNAL_ERROR", "message", "服务暂时不可用", "requestId", requestId()));
    }

    private String requestId() {
        return MDC.get("requestId") == null ? "" : MDC.get("requestId");
    }
}

