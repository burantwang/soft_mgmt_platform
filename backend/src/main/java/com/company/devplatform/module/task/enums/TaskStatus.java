package com.company.devplatform.module.task.enums;

import lombok.Getter;

/**
 * 问题单任务状态
 */
@Getter
public enum TaskStatus {

    PENDING(1, "待处理"),
    PROCESSING(2, "处理中"),
    COMPLETED(3, "已完成"),
    CLOSED(4, "已关闭");

    private final int code;
    private final String desc;

    TaskStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TaskStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (TaskStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }
}
