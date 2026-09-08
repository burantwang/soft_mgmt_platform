package com.company.devplatform.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户组表实体(组织归属,与角色权限解耦)
 */
@Data
@TableName("sys_group")
public class SysGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 组名(如 软件研发一处) */
    private String groupName;

    /** 备注 */
    private String remark;

    /** 状态:1启用 0停用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除:0未删 1已删 */
    private Integer isDeleted;
}
