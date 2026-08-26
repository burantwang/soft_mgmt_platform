package com.company.devplatform.module.release.enums;

import lombok.Getter;

/**
 * 发布来源枚举
 */
@Getter
public enum ReleaseSource {

    /** 人工上传 */
    MANUAL_UPLOAD(1, "人工上传"),
    /** Jenkins 推送 */
    JENKINS_PUSH(2, "Jenkins推送"),
    /** 手动创建 */
    MANUAL_CREATE(3, "手动创建");

    private final int code;
    private final String desc;

    ReleaseSource(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ReleaseSource of(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReleaseSource s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }
}
