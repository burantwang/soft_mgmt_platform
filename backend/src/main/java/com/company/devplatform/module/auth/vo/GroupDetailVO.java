package com.company.devplatform.module.auth.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 组详情(含已选成员ID,用于成员管理回填)
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GroupDetailVO extends GroupVO {

    /** 已选成员用户ID列表 */
    private List<Long> memberIds;
}
