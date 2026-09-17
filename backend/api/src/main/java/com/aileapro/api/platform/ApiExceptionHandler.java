package com.aileapro.api.platform;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

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

    /**
     * Malformed or mistyped requests, answered as the client errors they are.
     *
     * <p>Without these, Spring's own exceptions fall through to the catch-all below and a
     * caller who used the wrong verb or sent a bad UUID is told the server broke. That is not
     * a cosmetic difference: it sends people looking for a bug on the wrong side of the wire.
     */
    @ExceptionHandler({
        HttpRequestMethodNotSupportedException.class,
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorBody> handleBadRequest(Exception ex) {
        if (ex instanceof HttpRequestMethodNotSupportedException notAllowed) {
            return body(405, "method_not_allowed", notAllowed.getMessage());
        }
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            return body(400, "validation_error", mismatch.getName() + " is not in the expected form");
        }
        if (ex instanceof MissingServletRequestParameterException missing) {
            return body(400, "validation_error", missing.getParameterName() + " is required");
        }
        return body(400, "validation_error", "The request body could not be read");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorBody> handleNoRoute() {
        return body(404, "not_found", "No such route");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorBody> handleDenied() {
        return body(403, "forbidden", "Not allowed");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorBody> handleAuth() {
        return body(401, "unauthorized", "Authentication required");
    }

    /**
     * The last resort. The response stays deliberately vague, but the log does not: an
     * unexplained 500 with no stack trace anywhere is the one failure that cannot be
     * investigated afterwards, and the request id is what ties the two together.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorBody> handleOther(Exception ex, HttpServletRequest req) {
        log.error("Unhandled {} on {} {}", ex.getClass().getSimpleName(), req.getMethod(), req.getRequestURI(), ex);
        return body(500, "internal_error", "Unexpected error");
    }

    private ResponseEntity<ApiErrorBody> body(int status, String code, String message) {
        String rid = MDC.get("requestId");
        return ResponseEntity.status(status)
                .body(new ApiErrorBody(new ApiErrorBody.ErrorBody(code, message, rid)));
    }
}
