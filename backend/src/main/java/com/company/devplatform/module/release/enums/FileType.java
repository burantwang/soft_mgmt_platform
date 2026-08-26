package com.company.devplatform.module.release.enums;

import lombok.Getter;

/**
 * 文件类型枚举
 */
@Getter
public enum FileType {

    /** 测试报告 */
    REPORT(1, "测试报告"),
    /** 普通附件 */
    ATTACHMENT(2, "普通附件");

    private final int code;
    private final String desc;

    FileType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static FileType of(Integer code) {
        if (code == null) {
            return null;
        }
        for (FileType f : values()) {
            if (f.code == code) {
                return f;
            }
        }
        return null;
    }
}
