package com.company.devplatform.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务追踪-问题单明细（具体失败项/问题项）
 */
@Data
@TableName("issue_task_item")
public class IssueTaskItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 问题单任务ID */
    private Long taskId;

    /** 明细类别:failed失败 error错误 warning警告 note备注 */
    private String itemType;

    /** 明细名称(如用例名/问题项) */
    private String itemName;

    /** 原始信息/日志 */
    private String itemLog;

    /** 状态:1待处理 2处理中 3已修复 4非缺陷 */
    private Integer status;

    /** 责任人 */
    private Long assigneeId;

    /** 失败原因 */
    private String failReason;

    /** 修改方案 */
    private String fixPlan;

    /** 是否提Bug:0否 1是 */
    private Integer isBug;

    /** 分析进展 */
    private String progress;

    /** 结论 */
    private String conclusion;

    /** AI分析描述 */
    private String aiAnalysis;

    /** 处理完成时间 */
    private LocalDateTime handleTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
