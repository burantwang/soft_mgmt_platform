package com.company.devplatform.module.task.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DailySanity 失败任务追踪-失败明细项（映射 issue_task_item）
 * 字段与原始"失败任务追踪"（release_fail_case）保持一致
 */
@Data
public class TaskFailCaseVO {

    /** 明细ID */
    private Long id;

    /** 问题单任务ID */
    private Long taskId;

    /** 明细类别:failed失败 error错误 warning警告 note备注 */
    private String caseType;

    private String caseTypeDesc;

    /** 用例名/问题项名称 */
    private String caseName;

    /** 原始信息/日志 */
    private String caseLog;

    /** 状态:1待处理 2处理中 3已修复 4非缺陷 */
    private Integer status;

    private String statusDesc;

    private Long assigneeId;

    private String assigneeName;

    private String failReason;

    private String fixPlan;

    private Integer isBug;

    private String progress;

    private String conclusion;

    private String aiAnalysis;

    /** 发布时间（暂取明细创建时间，后续补充报告日期） */
    private LocalDateTime publishTime;
}
