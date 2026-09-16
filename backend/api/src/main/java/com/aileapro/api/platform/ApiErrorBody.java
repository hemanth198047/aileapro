package com.aileapro.api.platform;

public record ApiErrorBody(ErrorBody error) {
    public record ErrorBody(String code, String message, String requestId) {}
}
