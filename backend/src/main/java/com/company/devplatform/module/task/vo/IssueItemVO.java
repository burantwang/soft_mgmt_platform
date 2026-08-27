package com.company.devplatform.module.task.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问题单明细 VO
 */
@Data
public class IssueItemVO {

    private Long id;

    private Long taskId;

    private String itemType;

    private String itemTypeDesc;

    private String itemName;

    private String itemLog;

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

    private LocalDateTime handleTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
