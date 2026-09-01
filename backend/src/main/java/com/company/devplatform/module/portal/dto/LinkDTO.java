package com.company.devplatform.module.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 门户系统链接创建/更新入参
 */
@Data
public class LinkDTO {

    /** 所属板块ID */
    @NotNull(message = "请选择所属板块")
    private Long categoryId;

    /** 系统名称 */
    @NotBlank(message = "系统名称不能为空")
    @Size(max = 64, message = "系统名称不能超过64个字符")
    private String linkName;

    /** 访问地址：http/https 开头，或站内相对路径(如 /wiki) */
    @NotBlank(message = "访问地址不能为空")
    @Size(max = 500, message = "访问地址不能超过500个字符")
    @Pattern(regexp = "^(https?://|/).+", message = "访问地址需以 http://、https:// 或 / 开头")
    private String url;

    /** 系统简介 */
    @Size(max = 255, message = "系统简介不能超过255个字符")
    private String description;

    /** 图标(Element Plus 图标名或 emoji) */
    @Size(max = 64, message = "图标不能超过64个字符")
    private String icon;

    /** 主题色(hex) */
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "主题色格式不正确(如 #409EFF)")
    private String color;

    /** 排序(小值在前) */
    private Integer sort;

    /** 状态:1启用 0停用 */
    private Integer status;
}
