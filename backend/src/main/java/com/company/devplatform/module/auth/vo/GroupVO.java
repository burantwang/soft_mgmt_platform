package com.company.devplatform.module.auth.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组列表展示对象
 */
@Data
public class GroupVO {

    private Long id;

    /** 组名 */
    private String groupName;

    /** 备注 */
    private String remark;

    /** 状态:1启用 0停用 */
    private Integer status;

    /** 成员数量 */
    private Long memberCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
