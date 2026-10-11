package com.mealsubsidy.common.handler;

import com.mealsubsidy.common.exception.BizException;
import com.mealsubsidy.common.exception.BusinessException;
import com.mealsubsidy.common.result.Result;
import com.mealsubsidy.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({BusinessException.class, BizException.class})
    public Result<Void> handleBusiness(BizException ex) {
        if (ex.getCode() == BizException.CODE_OPTIMISTIC_LOCK) {
            log.warn("乐观锁冲突: code={}", ex.getCode());
            return Result.fail(BizException.CODE_OPTIMISTIC_LOCK, "并发冲突，请重试");
        }
        log.warn("业务异常: code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.BAD_REQUEST.getCode(),
                message.isEmpty() ? ResultCode.BAD_REQUEST.getMessage() : message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException ex) {
        String message = ex.getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.BAD_REQUEST.getCode(),
                message.isEmpty() ? ResultCode.BAD_REQUEST.getMessage() : message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraint(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.BAD_REQUEST.getCode(),
                message.isEmpty() ? ResultCode.BAD_REQUEST.getMessage() : message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleUnreadable() {
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception ex, HttpServletRequest request) {
        log.error("未预期异常, uri={}", request.getRequestURI(), ex);
        return Result.fail(ResultCode.SYSTEM_ERROR.getCode(), "系统内部错误");
    }

    private String formatFieldError(FieldError error) {
        String msg = error.getDefaultMessage();
        return msg == null ? error.getField() + "参数不合法" : msg;
    }
}
