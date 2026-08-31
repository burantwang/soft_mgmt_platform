package com.company.devplatform.module.release.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 失败用例明细实体
 */
@Data
@TableName("release_fail_case")
public class ReleaseFailCase {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 聚合任务ID */
    private Long taskId;

    /** 用例类型:failed失败 error错误 */
    private String caseType;

    /** 用例全名 */
    private String caseName;

    /** 用例运行日志 */
    private String caseLog;

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

    /** AI分析根因 */
    private String aiRootCause;

    /** AI分析佐证 */
    private String aiEvidence;

    /** AI解决建议 */
    private String aiSolution;

    /** Bug单号(Redmine) */
    private String bugNo;

    /** 问题分类 */
    private String issueCategory;

    /** 状态:1待处理 2处理中 3已完成 4已关闭 */
    private Integer status;

    /** 责任人(默认继承任务责任人) */
    private Long assigneeId;

    /** 处理时间 */
    private LocalDateTime handleTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除:0未删 1已删 */
    @TableLogic
    private Integer isDeleted;
}
