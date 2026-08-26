package com.company.devplatform.common.exception;

import com.company.devplatform.common.ErrorCode;
import lombok.Getter;

/**
 * 自定义业务异常
 * <p>用于业务规则校验失败时抛出，由全局异常处理器统一转换为友好提示返回前端，
 * 禁止直接向前端暴露 SQL 或底层堆栈信息。</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务错误码 */
    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
