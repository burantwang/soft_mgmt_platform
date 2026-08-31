package com.company.devplatform.common.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 「个人任务」失败用例统一视图
 * <p>聚合 DailySanity（release_fail_case）与 WeeklySanity（weekly_fail_case）中被指派给当前用户的用例，
 * 前端个人任务页按板块（board）分栏展示，字段与任务追踪处保持一致。</p>
 */
@Data
public class MyTaskCaseVO {

    /** 板块：daily / weekly（未来新增追踪板块时扩展） */
    private String board;

    /* ==================== 用例基本信息 ==================== */

    private Long id;

    /** 用例类型:failed失败 error错误 */
    private String caseType;

    private String caseTypeDesc;

    /** 用例全名 */
    private String caseName;

    /** 用例运行日志 */
    private String caseLog;

    /* ==================== 状态 ==================== */

    /** 状态:1待处理 2处理中 3已修复 4非缺陷 5已关闭 */
    private Integer status;

    private String statusDesc;

    /* ==================== 责任人 ==================== */

    private Long assigneeId;

    private String assigneeName;

    /* ==================== 处理字段 ==================== */

    private String failReason;

    private String fixPlan;

    private Integer isBug;

    /** 分析进展 */
    private String progress;

    /** 结论 */
    private String conclusion;

    /* ==================== AI 分析 ==================== */

    private String aiAnalysis;

    private Integer aiAnalysisCorrect;

    private String aiRootCause;

    private String aiEvidence;

    private String aiSolution;

    /** Bug单号(Redmine) */
    private String bugNo;

    /** 问题分类 */
    private String issueCategory;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /* ==================== 来源上下文 ==================== */

    /** 失败任务编号（daily） */
    private String taskNo;

    /** 代码分支 */
    private String branch;

    /** 镜像版本号 */
    private String version;

    /** 机型名 */
    private String projectName;

    /** 所属模块（weekly） */
    private String moduleName;

    /** 发布时间(daily)/报告上传时间(weekly) */
    private LocalDateTime publishTime;
}
