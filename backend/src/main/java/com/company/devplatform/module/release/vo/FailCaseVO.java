package com.company.devplatform.module.release.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 失败用例明细视图
 */
@Data
public class FailCaseVO {

    private Long id;

    private Long taskId;

    /** 用例类型:failed失败 error错误 */
    private String caseType;

    private String caseTypeDesc;

    /** 用例全名 */
    private String caseName;

    /** 用例运行日志 */
    private String caseLog;

    /** 状态:1待处理 2处理中 3已修复 4非缺陷 */
    private Integer status;

    private String statusDesc;

    /** 责任人 */
    private Long assigneeId;

    private String assigneeName;

    /** 失败原因(责任人填写) */
    private String failReason;

    /** 修改方案(责任人填写) */
    private String fixPlan;

    /** 是否提Bug:0否 1是 */
    private Integer isBug;

    /** 分析进展 */
    private String progress;

    /** 结论 */
    private String conclusion;

    /** AI辅助分析描述（原因分析、修改建议等） */
    private String aiAnalysis;

    /** AI分析是否正确:1是 0否 */
    private Integer aiAnalysisCorrect;

    /** 处理时间 */
    private LocalDateTime handleTime;
}
