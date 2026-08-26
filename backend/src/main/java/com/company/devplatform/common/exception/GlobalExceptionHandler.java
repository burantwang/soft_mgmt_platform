package com.company.devplatform.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.company.devplatform.common.ErrorCode;
import com.company.devplatform.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常统一拦截处理
 * <p>区分系统异常与业务异常；禁止直接抛出 SQL、底层堆栈信息返回前端。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("[业务异常] code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验异常（@Valid 注解） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError == null ? ErrorCode.PARAM_ERROR.getMsg() : fieldError.getDefaultMessage();
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    /** 未登录 */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.warn("[鉴权] 未登录: {}", e.getMessage());
        return Result.fail(ErrorCode.NOT_LOGIN);
    }

    /** 无操作权限 */
    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e) {
        log.warn("[鉴权] 无权限: {}", e.getMessage());
        return Result.fail(ErrorCode.NO_PERMISSION);
    }

    /** 无操作角色 */
    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e) {
        log.warn("[鉴权] 无角色: {}", e.getMessage());
        return Result.fail(ErrorCode.NO_PERMISSION);
    }

    /** 上传文件超限 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("[上传] 文件超出大小限制: {}", e.getMessage());
        return Result.fail(ErrorCode.FILE_TOO_LARGE);
    }

    /** 系统兜底异常（仅记录堆栈，不向前端暴露） */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("[系统异常]", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }
}
