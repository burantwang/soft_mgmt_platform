package com.company.devplatform.module.release.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 失败聚合任务视图（列表/详情）
 */
@Data
public class FailTaskVO {

    private Long id;

    /** 任务编号(如 FT20260811001) */
    private String taskNo;

    /** 发布记录ID */
    private Long recordId;

    /** 发布分支 */
    private String branch;

    /** 镜像版本 */
    private String version;

    /** 关联机型 */
    private List<String> projectNames;

    /** 状态:1待处理 2处理中 3已完成 4已关闭 */
    private Integer status;

    private String statusDesc;

    /** 责任人 */
    private Long assigneeId;

    private String assigneeName;

    /** 失败概述 */
    private String summary;

    /** 失败原因(汇总) */
    private String failReason;

    /** 修改方案(汇总) */
    private String fixPlan;

    /** 创建人 */
    private Long creatorId;

    private String creatorName;

    /** 用例统计 */
    private Integer caseTotal;

    private Integer casePending;

    private Integer caseProcessing;

    private Integer caseDone;

    /** 处理完成时间 */
    private LocalDateTime handleTime;

    private LocalDateTime createTime;
}
