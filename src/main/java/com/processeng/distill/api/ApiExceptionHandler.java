package com.processeng.distill.api;

import com.processeng.distill.api.dto.ErrorResponse;
import com.processeng.distill.error.DistillationException;
import com.processeng.distill.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 把所有异常统一翻译成结构化错误响应 {@link ErrorResponse}。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DistillationException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DistillationException ex) {
        return ResponseEntity
                .status(ex.code().status())
                .body(ErrorResponse.of(ex.code().name(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBeanValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(ApiExceptionHandler::formatFieldError)
                .toList();
        return ErrorResponse.of(ErrorCode.VALIDATION_ERROR.name(), "请求参数校验未通过", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadable(HttpMessageNotReadableException ex) {
        return ErrorResponse.of(ErrorCode.VALIDATION_ERROR.name(), "请求体不是合法的 JSON 或字段类型不正确");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleUnexpected(Exception ex) {
        return ErrorResponse.of("INTERNAL_ERROR", "服务内部错误：" + ex.getMessage());
    }

    private static String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
