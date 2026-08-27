package com.company.devplatform.module.task.vo;

import lombok.Data;

/**
 * 任务类型概览统计（Home / 列表页统计卡用）
 */
@Data
public class TaskTypeCountVO {

    private Long taskTypeId;

    private String taskTypeName;

    private String taskTypeCode;

    /** 任务总数 */
    private Long total;

    /** 待处理 */
    private Long pending;

    /** 处理中 */
    private Long processing;

    /** 已完成 */
    private Long completed;

    /** 已关闭 */
    private Long closed;
}
