package com.serverscope.web.api;

import java.time.Instant;

public record ApiEnvelope<T>(
        boolean ok,
        Instant timestamp,
        T data,
        ApiError error
) {
    public static <T> ApiEnvelope<T> success(T data) {
        return new ApiEnvelope<>(true, Instant.now(), data, null);
    }

    public static <T> ApiEnvelope<T> failure(String code, String message) {
        return new ApiEnvelope<>(false, Instant.now(), null, new ApiError(code, message));
    }
}
