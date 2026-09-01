package com.company.devplatform.module.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统门户链接实体
 */
@Data
@TableName("portal_link")
public class PortalLink {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属板块ID */
    private Long categoryId;

    /** 系统名称(如 GitLab / Jenkins) */
    private String linkName;

    /** 访问地址(http/https 开头，允许站内相对路径如 /wiki) */
    private String url;

    /** 系统简介 */
    private String description;

    /** 登录用户名(表格板块使用) */
    private String username;

    /** 登录密码(表格板块使用) */
    private String password;

    /** 图标(Element Plus 图标名或 emoji) */
    private String icon;

    /** 主题色(hex) */
    private String color;

    /** 排序(小值在前) */
    private Integer sort;

    /** 状态:1启用 0停用 */
    private Integer status;

    /** 创建人 */
    private Long creatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除:0未删 1已删 */
    @TableLogic
    private Integer isDeleted;
}
