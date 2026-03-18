package com.serverscope.web.api;

public record ApiError(
        String code,
        String message
) {
}
