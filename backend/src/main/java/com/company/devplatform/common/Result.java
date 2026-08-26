package com.company.devplatform.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 返回体
 * <p>固定结构：{"code": number, "msg": string, "data": any}</p>
 *
 * @param <T> 数据类型
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码：200 成功 */
    private int code;

    /** 提示信息 */
    private String msg;

    /** 返回数据 */
    private T data;

    public Result() {
    }

    public Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /** 成功（无数据） */
    public static <T> Result<T> ok() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), null);
    }

    /** 成功（带数据） */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), data);
    }

    /** 失败（自定义业务异常） */
    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    /** 失败（错误码枚举） */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMsg(), null);
    }
}
