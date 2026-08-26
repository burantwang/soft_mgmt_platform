package com.company.devplatform.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色表实体
 */
@Data
@TableName("sys_role")
public class SysRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色编码 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 备注 */
    private String remark;

    /** 状态:1正常 0停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除:0未删 1已删 */
    private Integer isDeleted;
}
