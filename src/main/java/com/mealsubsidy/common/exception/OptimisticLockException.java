package com.mealsubsidy.common.exception;

import com.mealsubsidy.common.result.ResultCode;

/**
 * 乐观锁冲突：CAS 更新影响行数为 0。
 */
public class OptimisticLockException extends BusinessException {

    public static final String MESSAGE = "并发冲突，请重试";

    public OptimisticLockException() {
        super(ResultCode.BUSINESS_ERROR, MESSAGE);
    }

    public OptimisticLockException(String message) {
        super(ResultCode.BUSINESS_ERROR, message);
    }
}
