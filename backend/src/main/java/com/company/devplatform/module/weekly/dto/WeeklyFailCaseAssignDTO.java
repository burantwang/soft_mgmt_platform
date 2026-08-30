package com.company.devplatform.module.weekly.dto;

import lombok.Data;

/**
 * WeeklySanity 失败用例快速指派入参
 */
@Data
public class WeeklyFailCaseAssignDTO {

    /** 责任人ID（null 表示取消指派） */
    private Long assigneeId;
}
