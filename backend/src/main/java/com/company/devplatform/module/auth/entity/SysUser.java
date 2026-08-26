package com.company.devplatform.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表实体
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** 密码(BCrypt 加密) */
    private String password;

    /** 姓名 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 状态:1正常 0禁用 */
    private Integer status;

    /** 是否强制改密:1是 0否 */
    private Integer mustChangePwd;

    /** 备注 */
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除:0未删 1已删 */
    private Integer isDeleted;
}
