package com.company.devplatform.module.task.enums;

import lombok.Getter;

/**
 * 问题单明细状态
 */
@Getter
public enum TaskItemStatus {

    PENDING(1, "待处理"),
    PROCESSING(2, "处理中"),
    FIXED(3, "已修复"),
    NOT_DEFECT(4, "非缺陷");

    private final int code;
    private final String desc;

    TaskItemStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TaskItemStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (TaskItemStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }
}
