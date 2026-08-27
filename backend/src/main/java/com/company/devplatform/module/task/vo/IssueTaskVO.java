package com.company.devplatform.module.task.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问题单任务 列表 VO
 */
@Data
public class IssueTaskVO {

    private Long id;

    private String taskNo;

    private Long taskTypeId;

    private String taskTypeName;

    private String title;

    private Integer status;

    private String statusDesc;

    private Integer priority;

    private String priorityDesc;

    private String branch;

    private String version;

    private String projectName;

    private Long assigneeId;

    private String assigneeName;

    private Long creatorId;

    private String creatorName;

    private LocalDateTime handleTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 明细总数 */
    private Long itemTotal;

    /** 待处理明细数 */
    private Long itemPending;

    /** 处理中明细数 */
    private Long itemProcessing;

    /** 已完结明细数(已修复+非缺陷) */
    private Long itemDone;
}
