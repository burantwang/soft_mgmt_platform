package com.company.devplatform.common;

import lombok.Getter;

/**
 * 错误码枚举
 * <p>约定：业务成功 code=200；自定义业务错误码区间 10001-19999；鉴权相关错误码 40xxx</p>
 */
@Getter
public enum ErrorCode {

    /** 成功 */
    SUCCESS(200, "操作成功"),

    /* ---------- 业务错误码 10001-19999 ---------- */
    PARAM_ERROR(10001, "参数错误"),
    DATA_NOT_FOUND(10002, "数据不存在或已被删除"),
    BUSINESS_ERROR(10003, "业务处理失败"),
    DATA_EXIST(10004, "数据已存在"),
    FILE_TYPE_NOT_ALLOWED(10005, "文件类型不允许上传"),
    FILE_TOO_LARGE(10006, "文件大小超出限制"),
    FILE_EMPTY(10007, "上传文件为空"),
    HTML_PARSE_ERROR(10008, "测试报告解析失败"),
    REPORT_FORMAT_INVALID(10009, "非标准测试报告，仅支持 pytest-html 格式"),
    FILE_SAVE_ERROR(10010, "文件保存失败"),
    FILE_READ_ERROR(10011, "文件读取失败"),
    PROJECT_REQUIRED(10012, "请至少选择一个机型"),
    RELEASE_EDIT_NOT_ALLOWED(10013, "该记录不允许编辑"),

    /* ---------- 鉴权相关错误码 40xxx ---------- */
    NOT_LOGIN(40100, "未登录或登录已失效"),
    NO_PERMISSION(40101, "无操作权限"),
    LOGIN_FAILED(40102, "账号或密码错误"),
    USER_DISABLED(40103, "账号已被禁用"),
    MUST_CHANGE_PWD(40104, "请先修改初始密码"),
    TOKEN_INVALID(40105, "接口调用凭证无效"),

    /* ---------- 系统异常 ---------- */
    SYSTEM_ERROR(500, "系统异常，请稍后重试");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
