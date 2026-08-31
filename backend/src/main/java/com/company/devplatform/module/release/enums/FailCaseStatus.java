package com.company.devplatform.module.release.enums;

import lombok.Getter;

/**
 * 失败用例明细状态枚举
 * <p>任务状态由明细状态联动推导，规则见 ReleaseFailTaskServiceImpl.refreshTaskStatus</p>
 */
@Getter
public enum FailCaseStatus {

    /** 待处理 */
    PENDING(1, "待处理"),
    /** 处理中 */
    PROCESSING(2, "处理中"),
    /** 已修复 */
    FIXED(3, "已修复"),
    /** 非缺陷（无需处理） */
    NOT_DEFECT(4, "非缺陷"),
    /** 已关闭：管理员识别为非问题后直接关闭，从分析完成率分母排除 */
    CLOSED(5, "已关闭");

    private final int code;
    private final String desc;

    FailCaseStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static FailCaseStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (FailCaseStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }
}
