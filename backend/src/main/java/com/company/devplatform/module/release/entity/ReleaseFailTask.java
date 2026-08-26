package com.company.devplatform.module.release.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 失败聚合任务实体
 */
@Data
@TableName("release_fail_task")
public class ReleaseFailTask {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务编号(如 FT20260811001) */
    private String taskNo;

    /** 发布记录ID */
    private Long recordId;

    /** 状态:1待处理 2处理中 3已完成 4已关闭 */
    private Integer status;

    /** 任务责任人(用户ID) */
    private Long assigneeId;

    /** 失败概述 */
    private String summary;

    /** 失败原因(汇总) */
    private String failReason;

    /** 修改方案(汇总) */
    private String fixPlan;

    /** 创建人 */
    private Long creatorId;

    /** 处理完成时间 */
    private LocalDateTime handleTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除:0未删 1已删 */
    @TableLogic
    private Integer isDeleted;
}
