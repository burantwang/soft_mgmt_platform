package com.company.devplatform.module.auth.dto;

import lombok.Data;

import java.util.List;

/**
 * 组成员整体替换参数
 */
@Data
public class GroupMembersDTO {

    /** 成员用户ID列表(整体替换,传空表示清空所有成员) */
    private List<Long> userIds;
}
