package com.mealsubsidy.common.exception;

import com.mealsubsidy.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常，与 {@link BizException} 并存，供全局异常处理器统一捕获。
 */
@Getter
public class BusinessException extends BizException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode);
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }

    public BusinessException(int code, String message) {
        super(code, message);
    }
}
