package com.company.devplatform.module.task.dto;

import lombok.Data;

/**
 * 问题单明细（创建/编辑共用）
 */
@Data
public class IssueItemDTO {

    /** 编辑时必填 */
    private Long id;

    /** 明细类别:failed失败 error错误 warning警告 note备注 */
    private String itemType = "failed";

    private String itemName;

    private String itemLog;

    private Integer status = 1;

    private Long assigneeId;

    private String failReason;

    private String fixPlan;

    private Integer isBug = 0;

    private String progress;

    private String conclusion;

    private String aiAnalysis;
}
