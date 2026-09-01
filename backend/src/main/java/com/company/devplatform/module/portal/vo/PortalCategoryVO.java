package com.company.devplatform.module.portal.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 门户板块 VO（含其下链接）
 */
@Data
public class PortalCategoryVO {

    /** 板块ID */
    private Long id;

    /** 板块名称 */
    private String categoryName;

    /** 板块图标 */
    private String icon;

    /** 主题色 */
    private String color;

    /** 板块描述 */
    private String description;

    /** 展示方式:card卡片 table表格 */
    private String layout;

    /** 排序 */
    private Integer sort;

    /** 状态:1启用 0停用 */
    private Integer status;

    /** 板块下链接总数(不含逻辑删除) */
    private Integer linkCount;

    /** 板块下链接列表 */
    private List<PortalLinkVO> links = new ArrayList<>();
}
