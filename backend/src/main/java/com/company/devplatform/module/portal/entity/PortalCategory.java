package com.company.devplatform.module.portal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统门户板块实体
 */
@Data
@TableName("portal_category")
public class PortalCategory {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 板块名称(如 研发相关 / 测试相关) */
    private String categoryName;

    /** 板块图标(Element Plus 图标名或 emoji) */
    private String icon;

    /** 主题色(hex) */
    private String color;

    /** 板块描述 */
    private String description;

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
