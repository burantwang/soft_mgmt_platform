package com.company.devplatform.module.task.enums;

import lombok.Getter;

/**
 * 任务优先级
 */
@Getter
public enum TaskPriority {

    LOW(1, "低"),
    MEDIUM(2, "中"),
    HIGH(3, "高");

    private final int code;
    private final String desc;

    TaskPriority(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TaskPriority of(Integer code) {
        if (code == null) {
            return null;
        }
        for (TaskPriority p : values()) {
            if (p.code == code) {
                return p;
            }
        }
        return null;
    }
}
