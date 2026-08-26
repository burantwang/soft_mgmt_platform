package com.company.devplatform.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限点表实体
 */
@Data
@TableName("sys_permission")
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权限编码(如 sonic:edit) */
    private String permCode;

    /** 权限名称 */
    private String permName;

    /** 所属模块(sonic/wiki/system) */
    private String module;

    /** 模块名称 */
    private String moduleName;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除:0未删 1已删 */
    private Integer isDeleted;
}
