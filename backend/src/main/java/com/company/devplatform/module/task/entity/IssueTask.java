package com.company.devplatform.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务追踪-问题单任务（通用化：可追踪任意类型任务）
 */
@Data
@TableName("issue_task")
public class IssueTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务编号(如 ATDD20260827001) */
    private String taskNo;

    /** 任务类型ID */
    private Long taskTypeId;

    /** 任务标题/概述 */
    private String title;

    /** 状态:1待处理 2处理中 3已完成 4已关闭 */
    private Integer status;

    /** 优先级:1低 2中 3高 */
    private Integer priority;

    /** 关联代码分支(可选) */
    private String branch;

    /** 关联版本号(可选) */
    private String version;

    /** 关联项目/机型(可选) */
    private String projectName;

    /** 责任人(用户ID) */
    private Long assigneeId;

    /** 创建人(用户ID) */
    private Long creatorId;

    /** 问题描述 */
    private String summary;

    /** 原因分析(汇总) */
    private String reason;

    /** 解决方案(汇总) */
    private String fixPlan;

    /** 完成时间 */
    private LocalDateTime handleTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
