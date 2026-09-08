package com.company.devplatform.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户-组关联实体(多对多)
 */
@Data
@TableName("sys_group_user")
public class SysGroupUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 组ID */
    private Long groupId;

    /** 用户ID */
    private Long userId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除:0未删 1已删 */
    private Integer isDeleted;
}
