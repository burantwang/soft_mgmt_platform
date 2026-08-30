package com.company.devplatform.module.weekly.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * WeeklySanity 周度失败用例明细实体
 * <p>关联 weekly_report，每个失败/错误用例一条记录。</p>
 */
@Data
@TableName("weekly_fail_case")
public class WeeklyFailCase {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 周度报告ID(weekly_report.id) */
    private Long reportId;

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

    /** AI辅助分析描述 */
    private String aiAnalysis;

    /** AI分析是否正确:1是 0否 */
    private Integer aiAnalysisCorrect;

    /** 状态:1待处理 2处理中 3已修复 4非缺陷 */
    private Integer status;

    /** 责任人(用户ID) */
    private Long assigneeId;

    /** 处理时间 */
    private LocalDateTime handleTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除:0未删 1已删 */
    @TableLogic
    private Integer isDeleted;
}
