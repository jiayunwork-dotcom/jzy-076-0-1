package com.processeng.distill.api.dto;

import java.time.Instant;
import java.util.List;

/**
 * 统一结构化错误响应。
 */
public record ErrorResponse(String code,
                            String message,
                            List<String> details,
                            Instant timestamp) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, List.of(), Instant.now());
    }

    public static ErrorResponse of(String code, String message, List<String> details) {
        return new ErrorResponse(code, message, List.copyOf(details), Instant.now());
    }
}
