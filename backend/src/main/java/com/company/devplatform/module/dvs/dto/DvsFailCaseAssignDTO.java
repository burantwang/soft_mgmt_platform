package com.company.devplatform.module.dvs.dto;

import lombok.Data;

/**
 * DVS 失败用例快速指派入参
 */
@Data
public class DvsFailCaseAssignDTO {

    /** 责任人ID（null 表示取消指派） */
    private Long assigneeId;
}
