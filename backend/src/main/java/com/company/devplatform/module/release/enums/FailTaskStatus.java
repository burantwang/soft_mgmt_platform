package com.company.devplatform.module.release.enums;

import lombok.Getter;

/**
 * 失败聚合任务状态枚举
 */
@Getter
public enum FailTaskStatus {

    /** 待处理 */
    PENDING(1, "待处理"),
    /** 处理中 */
    PROCESSING(2, "处理中"),
    /** 已完成 */
    COMPLETED(3, "已完成"),
    /** 已关闭 */
    CLOSED(4, "已关闭");

    private final int code;
    private final String desc;

    FailTaskStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static FailTaskStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (FailTaskStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }
}
