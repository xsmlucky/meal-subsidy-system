package com.mealsubsidy.common.exception;

import com.mealsubsidy.common.result.ResultCode;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    /** 乐观锁并发冲突 */
    public static final int CODE_OPTIMISTIC_LOCK = 50001;

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
