package com.company.devplatform.module.auth.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色 VO
 */
@Data
public class RoleVO {

    private Long id;

    private String roleCode;

    private String roleName;

    private String remark;

    /** 状态:1正常 0停用 */
    private Integer status;

    /** 已分配权限点ID列表 */
    private List<Long> permissionIds;

    /** 关联用户数 */
    private Long userCount;

    private LocalDateTime createTime;
}
