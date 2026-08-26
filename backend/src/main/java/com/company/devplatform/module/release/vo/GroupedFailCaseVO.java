package com.company.devplatform.module.release.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 失败用例分组明细项
 */
@Data
public class GroupedFailCaseVO {

    private Long id;

    private Long taskId;

    private Long recordId;

    private String caseType;

    private String caseTypeDesc;

    private String caseName;

    private String caseLog;

    private Integer status;

    private String statusDesc;

    private Long assigneeId;

    private String assigneeName;

    private String failReason;

    private String fixPlan;

    private Integer isBug;

    private String progress;

    private String conclusion;

    private LocalDateTime publishTime;
}
