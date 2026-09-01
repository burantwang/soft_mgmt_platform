package com.company.devplatform.module.portal.vo;

import lombok.Data;

/**
 * 门户系统链接 VO
 */
@Data
public class PortalLinkVO {

    /** 链接ID */
    private Long id;

    /** 所属板块ID */
    private Long categoryId;

    /** 系统名称 */
    private String linkName;

    /** 访问地址 */
    private String url;

    /** 系统简介 */
    private String description;

    /** 登录用户名 */
    private String username;

    /** 登录密码 */
    private String password;

    /** 图标 */
    private String icon;

    /** 主题色 */
    private String color;

    /** 排序 */
    private Integer sort;

    /** 状态:1启用 0停用 */
    private Integer status;
}
