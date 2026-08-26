package com.company.devplatform.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色-权限关联表实体
 */
@Data
@TableName("sys_role_permission")
public class SysRolePermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色ID */
    private Long roleId;

    /** 权限点ID */
    private Long permissionId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除:0未删 1已删 */
    private Integer isDeleted;
}
