package com.company.devplatform.module.release.enums;

import lombok.Getter;

/**
 * 发布结果枚举
 */
@Getter
public enum ReleaseResult {

    /** 成功 */
    SUCCESS(1, "成功"),
    /** 失败 */
    FAILED(2, "失败");

    private final int code;
    private final String desc;

    ReleaseResult(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ReleaseResult of(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReleaseResult r : values()) {
            if (r.code == code) {
                return r;
            }
        }
        return null;
    }
}
