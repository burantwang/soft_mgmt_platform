package com.company.devplatform.module.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 门户板块创建/更新入参
 */
@Data
public class CategoryDTO {

    /** 板块名称 */
    @NotBlank(message = "板块名称不能为空")
    @Size(max = 64, message = "板块名称不能超过64个字符")
    private String categoryName;

    /** 板块图标(Element Plus 图标名或 emoji) */
    @Size(max = 64, message = "图标不能超过64个字符")
    private String icon;

    /** 主题色(hex) */
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "主题色格式不正确(如 #409EFF)")
    private String color;

    /** 板块描述 */
    @Size(max = 255, message = "板块描述不能超过255个字符")
    private String description;

    /** 展示方式:card卡片 table表格 */
    @Pattern(regexp = "^(card|table)$", message = "展示方式不正确(仅支持 card/table)")
    private String layout;

    /** 排序(小值在前) */
    private Integer sort;

    /** 状态:1启用 0停用 */
    private Integer status;
}
