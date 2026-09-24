package com.processeng.distill.error;

/**
 * 所有业务/领域错误统一走该异常，由全局异常处理器映射为结构化响应。
 */
public class DistillationException extends RuntimeException {

    private final ErrorCode code;

    public DistillationException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
