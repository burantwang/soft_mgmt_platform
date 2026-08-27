package com.company.devplatform.module.task.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务类型 VO
 */
@Data
public class TaskTypeVO {

    private Long id;

    private String code;

    private String name;

    private String icon;

    private Integer sort;

    private Integer enabled;

    private String remark;

    private LocalDateTime createTime;
}
