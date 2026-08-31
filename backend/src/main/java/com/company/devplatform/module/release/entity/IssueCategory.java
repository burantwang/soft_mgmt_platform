package com.company.devplatform.module.release.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问题分类
 */
@Data
@TableName("issue_category")
public class IssueCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称(展示) */
    private String categoryName;

    /** 分类编码 */
    private String categoryCode;

    /** 状态:1启用 0停用 */
    private Integer status;

    /** 排序 */
    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
