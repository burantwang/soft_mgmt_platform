package com.company.devplatform.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务追踪-任务类型
 */
@Data
@TableName("task_type")
public class TaskType {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 类型编码(唯一，如 atdd/dvs/pylint/jingan) */
    private String code;

    /** 类型名称 */
    private String name;

    /** 图标标识 */
    private String icon;

    /** 排序(越小越靠前) */
    private Integer sort;

    /** 启用:1启用 0停用 */
    private Integer enabled;

    /** 备注 */
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
