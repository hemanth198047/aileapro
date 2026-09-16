package com.ailepro.api.platform;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorBody> handleApi(ApiException ex) {
        return body(ex.getStatus(), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorBody> handleValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse("Invalid request");
        return body(400, "validation_error", msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorBody> handleConstraint(ConstraintViolationException ex) {
        return body(400, "validation_error", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorBody> handleDenied() {
        return body(403, "forbidden", "Not allowed");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorBody> handleAuth() {
        return body(401, "unauthorized", "Authentication required");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorBody> handleOther(Exception ex, HttpServletRequest req) {
        return body(500, "internal_error", "Unexpected error");
    }

    private ResponseEntity<ApiErrorBody> body(int status, String code, String message) {
        String rid = MDC.get("requestId");
        return ResponseEntity.status(status)
                .body(new ApiErrorBody(new ApiErrorBody.ErrorBody(code, message, rid)));
    }
}
