package com.company.devplatform.module.release.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目(机型)实体
 */
@Data
@TableName("project")
public class ReleaseProject {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目/机型名称(如 Gaea) */
    private String projectName;

    /** 项目编码 */
    private String projectCode;

    /** 描述 */
    private String description;

    /** 状态:1启用 0停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除:0未删 1已删 */
    @TableLogic
    private Integer isDeleted;
}
