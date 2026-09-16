package com.aileapro.api.platform;

public class ApiException extends RuntimeException {
    private final String code;
    private final int status;

    public ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public String getCode() { return code; }
    public int getStatus() { return status; }

    public static ApiException unauthorized() {
        return new ApiException(401, "unauthorized", "Authentication required");
    }

    public static ApiException forbidden() {
        return new ApiException(403, "forbidden", "Not allowed");
    }

    public static ApiException conflict(String message) {
        return new ApiException(409, "conflict", message);
    }

    public static ApiException validation(String message) {
        return new ApiException(400, "validation_error", message);
    }

    public static ApiException rateLimited() {
        return new ApiException(429, "rate_limited", "Too many attempts");
    }
}
