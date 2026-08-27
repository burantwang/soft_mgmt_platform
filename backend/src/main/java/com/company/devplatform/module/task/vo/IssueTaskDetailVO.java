package com.company.devplatform.module.task.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 问题单任务 详情 VO（含明细）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IssueTaskDetailVO extends IssueTaskVO {

    private String summary;

    private String reason;

    private String fixPlan;

    private List<IssueItemVO> items;
}
